/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;


/**
 * AddressObservationWriter manages the MD-SAL data tree for address observations (mac, ip) on each node-connector.
 */
public class AddressObservationWriter {

  private Logger _logger = LoggerFactory.getLogger(AddressObservationWriter.class);

  private AtomicLong addressKey = new AtomicLong(0);
  private long timestampUpdateInterval;
  private DataBroker dataService;
  private Map<NodeConnectorRef, NodeConnectorLock> lockMap = new HashMap<>();
  private Map<NodeConnectorLock, CheckedFuture> futureMap = new HashMap<>();

  private class NodeConnectorLock {

  }

  /**
   * Construct an AddressTracker with the specified inputs
   *
   * @param dataService The DataBrokerService for the AddressTracker
   */
  public AddressObservationWriter(DataBroker dataService) {
    this.dataService = dataService;
  }

  public void setTimestampUpdateInterval(long timestampUpdateInterval) {
    this.timestampUpdateInterval = timestampUpdateInterval;
  }

  /**
   * Add addresses into the MD-SAL data tree
   *
   * @param macAddress       The MacAddress of the new L2Address object
   * @param nodeConnectorRef The NodeConnectorRef of the new L2Address object
   */
  public void addAddress(MacAddress macAddress, IpAddress ipAddress, NodeConnectorRef nodeConnectorRef) {
    if(macAddress == null || ipAddress == null || nodeConnectorRef == null) {
      return;
    }

    // get the lock for given node connector so at a time only one observation can be made on a node connector
    NodeConnectorLock nodeConnectorLock;
    synchronized(this) {
      nodeConnectorLock = lockMap.get(nodeConnectorRef);
      if(nodeConnectorLock == null) {
        nodeConnectorLock = new NodeConnectorLock();
        lockMap.put(nodeConnectorRef, nodeConnectorLock);
      }

    }

    synchronized(nodeConnectorLock) {
      // Ensure previous transaction finished writing to the db
      CheckedFuture<Void, TransactionCommitFailedException> future = futureMap.get(nodeConnectorLock);
      if (future != null) {
        try {
          future.get();
        }
        catch (InterruptedException|ExecutionException e) {
          _logger.error("Exception while waiting for previous transaction to finish", e);
        }
      }

      // Initialize builders
      long now = new Date().getTime();
      final AddressCapableNodeConnectorBuilder acncBuilder = new AddressCapableNodeConnectorBuilder();
      final AddressesBuilder addressBuilder = new AddressesBuilder()
          .setIp(ipAddress)
          .setMac(macAddress)
          .setFirstSeen(now)
          .setLastSeen(now);
      List<Addresses> addresses = null;

      // Read existing address observations from data tree
      ReadOnlyTransaction readTransaction = dataService.newReadOnlyTransaction();

      NodeConnector nc = null;
      try {
        Optional<NodeConnector> dataObjectOptional = readTransaction.read(LogicalDatastoreType.OPERATIONAL, (InstanceIdentifier<NodeConnector>) nodeConnectorRef.getValue()).get();
        if(dataObjectOptional.isPresent())
          nc = (NodeConnector) dataObjectOptional.get();
      } catch(Exception e) {
        _logger.error("Error reading node connector {}", nodeConnectorRef.getValue());
        readTransaction.close();
        throw new RuntimeException("Error reading from operational store, node connector : " + nodeConnectorRef, e);
      }
      readTransaction.close();
      if(nc == null) {
        return;
      }
      AddressCapableNodeConnector acnc = (AddressCapableNodeConnector) nc.getAugmentation(AddressCapableNodeConnector.class);


      // Address observations exist
      if(acnc != null && acnc.getAddresses() != null) {
        // Search for this mac-ip pair in the existing address observations & update last-seen timestamp
        addresses = acnc.getAddresses();
        for(int i = 0; i < addresses.size(); i++) {
          if(addresses.get(i).getIp().equals(ipAddress) && addresses.get(i).getMac().equals(macAddress)) {
            if((now - addresses.get(i).getLastSeen()) > timestampUpdateInterval) {
              addressBuilder.setFirstSeen(addresses.get(i).getFirstSeen())
                  .setKey(addresses.get(i).getKey());
              addresses.remove(i);
              break;
            } else {
              return;
            }
          }
        }
      }
      // Address observations don't exist, so create the list
      else {
        addresses = new ArrayList<>();
      }

      if(addressBuilder.getKey() == null) {
        addressBuilder.setKey(new AddressesKey(BigInteger.valueOf(addressKey.getAndIncrement())));
      }

      // Add as an augmentation
      addresses.add(addressBuilder.build());
      acncBuilder.setAddresses(addresses);

      //build Instance Id for AddressCapableNodeConnector
      InstanceIdentifier<AddressCapableNodeConnector> addressCapableNcInstanceId =
          ((InstanceIdentifier<NodeConnector>) nodeConnectorRef.getValue())
              .augmentation(AddressCapableNodeConnector.class);
      final WriteTransaction writeTransaction = dataService.newWriteOnlyTransaction();
      // Update this AddressCapableNodeConnector in the MD-SAL data tree
      writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, addressCapableNcInstanceId, acncBuilder.build());
      final CheckedFuture writeTxResultFuture = writeTransaction.submit();
      Futures.addCallback(writeTxResultFuture, new FutureCallback() {
        @Override
        public void onSuccess(Object o) {
          _logger.debug("AddressObservationWriter write successful for tx :{}", writeTransaction.getIdentifier());
        }

        @Override
        public void onFailure(Throwable throwable) {
          _logger.error("AddressObservationWriter write transaction {} failed", writeTransaction.getIdentifier(), throwable.getCause());
        }
      });
      futureMap.put(nodeConnectorLock, writeTxResultFuture);
    }
  }
}
