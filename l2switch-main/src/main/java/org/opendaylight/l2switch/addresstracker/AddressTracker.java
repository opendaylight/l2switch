/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker;

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AddressTracker manages the MD-SAL data tree for address observations (mac, ip) on each node-connector.
 */
public class AddressTracker {

  private DataBrokerService dataService;

  /**
   * Construct an AddressTracker with the specified inputs
   * @param dataService  The DataBrokerService for the AddressTracker
   */
  public AddressTracker(DataBrokerService dataService) {
    this.dataService = dataService;
  }

  /**
   * Add addresses into the MD-SAL data tree
   * @param macAddress  The MacAddress of the new L2Address object
   * @param nodeConnectorRef  The NodeConnectorRef of the new L2Address object
   */
  public void addAddress(MacAddress macAddress, IpAddress ipAddress, NodeConnectorRef nodeConnectorRef) {
    System.out.println(macAddress);
    System.out.println(ipAddress);
    System.out.println(nodeConnectorRef);
    System.out.println("====");
    if(macAddress == null || ipAddress == null || nodeConnectorRef == null) {
      return;
    }
    Timestamp now = new Timestamp(new Date().getTime());
    NodeConnectorBuilder ncBuilder = null;

    // Read existing address observations from data tree
    NodeConnector nc = (NodeConnector)dataService.readOperationalData(nodeConnectorRef.getValue());
    AddressCapableNodeConnector acnc = (AddressCapableNodeConnector)nc.getAugmentation(AddressCapableNodeConnector.class);

    // Address observations exist
    if (acnc != null) {
      List<Addresses> addresses = acnc.getAddresses();
      // Search for this mac-ip pair in the existing address observations & update last-seen timestamp
      for (int i = 0; i < addresses.size(); i++) {
        if (addresses.get(i).getIp().equals(ipAddress) && addresses.get(i).getMac().equals(macAddress)) {
          addresses.add(new AddressesBuilder()
            .setIp(ipAddress)
            .setMac(macAddress)
            .setFirstSeen(addresses.get(i).getFirstSeen())
            .setLastSeen(now)
            .build());
          addresses.remove(i);
          return;
        }
      }

      // This mac-ip pair is not part of the list, so add it to the end of the list
      addresses.add(new AddressesBuilder()
        .setMac(macAddress)
        .setIp(ipAddress)
        .setFirstSeen(now)
        .setLastSeen(now)
        .build());

      ncBuilder = new NodeConnectorBuilder(nc);
    }
    // Address observations don't exist, so create the list
    else {
      // Create AddressCapableNodeConnector
      final AddressCapableNodeConnectorBuilder builder = new AddressCapableNodeConnectorBuilder();
      ArrayList<Addresses> addresses = new ArrayList<>();
      addresses.add(new AddressesBuilder()
        .setMac(macAddress)
        .setIp(ipAddress)
        .setFirstSeen(now)
        .setLastSeen(now)
        .build());
      builder.setAddresses(addresses);

      // Add as an augmentation
      ncBuilder = new NodeConnectorBuilder(nc)
        .addAugmentation(AddressCapableNodeConnector.class, builder.build());
    }

    // Update this NodeConnector in the MD-SAL data tree
    final DataModificationTransaction it = dataService.beginTransaction();
    it.putOperationalData(nodeConnectorRef.getValue(), ncBuilder.build());
    it.commit();
  }
}
