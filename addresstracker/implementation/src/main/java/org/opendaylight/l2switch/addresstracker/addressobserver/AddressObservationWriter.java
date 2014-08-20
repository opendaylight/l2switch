/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AddressObservationWriter manages the MD-SAL data tree for address observations (mac, ip) on each node-connector.
 */
public class AddressObservationWriter {

  private Logger _logger = LoggerFactory.getLogger(AddressObservationWriter.class);

  private AtomicLong addressKey = new AtomicLong(0);

  private DataBroker dataService;
  private Map<NodeConnectorRef, NodeConnectorLock> lockMap = new HashMap<>();

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
      // Initialize builders
      long now = new Date().getTime();
      final AddressCapableNodeConnectorBuilder acncBuilder = new AddressCapableNodeConnectorBuilder();
      final AddressesBuilder addressBuilder = new AddressesBuilder()
          .setIp(ipAddress)
          .setMac(macAddress)
          .setFirstSeen(now)
          .setLastSeen(now)
          .setKey(new AddressesKey(BigInteger.valueOf(addressKey.getAndIncrement())));
      List<Addresses> addresses = null;

      // Read existing address observations from data tree
      ReadWriteTransaction readWriteTransaction = dataService.newReadWriteTransaction();

      NodeConnector nc = null;
      try {
        Optional<NodeConnector> dataObjectOptional = readWriteTransaction.read(LogicalDatastoreType.OPERATIONAL, (InstanceIdentifier<NodeConnector>) nodeConnectorRef.getValue()).get();
        if(dataObjectOptional.isPresent())
          nc = (NodeConnector) dataObjectOptional.get();
      } catch(Exception e) {
        _logger.error("Error reading node connector {}", nodeConnectorRef.getValue());
        readWriteTransaction.submit();
        throw new RuntimeException("Error reading from operational store, node connector : " + nodeConnectorRef, e);
      }
      if(nc == null) {
        readWriteTransaction.submit();
        return;
      }
      AddressCapableNodeConnector acnc = (AddressCapableNodeConnector) nc.getAugmentation(AddressCapableNodeConnector.class);


      // Address observations exist
      if(acnc != null && acnc.getAddresses() != null) {
        // Search for this mac-ip pair in the existing address observations & update last-seen timestamp
        addresses = acnc.getAddresses();
        for(int i = 0; i < addresses.size(); i++) {
          if(addresses.get(i).getIp().equals(ipAddress) && addresses.get(i).getMac().equals(macAddress)) {
            addressBuilder.setFirstSeen(addresses.get(i).getFirstSeen())
                .setKey(addresses.get(i).getKey());
            addresses.remove(i);
            break;
          }
        }
      }
      // Address observations don't exist, so create the list
      else {
        addresses = new ArrayList<>();
      }

      // Add as an augmentation
      addresses.add(addressBuilder.build());
      acncBuilder.setAddresses(addresses);
      NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder(nc)
          .setKey(nc.getKey())
          .addAugmentation(AddressCapableNodeConnector.class, acncBuilder.build());

      // Update this NodeConnector in the MD-SAL data tree
      readWriteTransaction.put(LogicalDatastoreType.OPERATIONAL, (InstanceIdentifier<NodeConnector>) nodeConnectorRef.getValue(), ncBuilder.build());
      readWriteTransaction.submit();
    }
  }

}
