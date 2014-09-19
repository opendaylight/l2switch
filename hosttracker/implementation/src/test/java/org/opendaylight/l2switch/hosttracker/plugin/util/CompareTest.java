/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.util;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPoints;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.VlanId;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompareTest {

  @Test
  public void testAddresses() throws Exception {
    Addresses a1 = new AddressesBuilder()
      .setIp(new IpAddress("1.1.1.1".toCharArray()))
      .setMac(new MacAddress("aa:aa:aa:aa:aa:aa"))
      .setVlan(new VlanId(1))
      .build();
    Addresses a2 = new AddressesBuilder()
      .setIp(new IpAddress("1.1.1.1".toCharArray()))
      .setMac(new MacAddress("aa:aa:aa:aa:aa:aa"))
      .setVlan(new VlanId(1))
      .build();
    Addresses diffIp = new AddressesBuilder()
      .setIp(new IpAddress("1.1.1.2".toCharArray()))
      .setMac(new MacAddress("aa:aa:aa:aa:aa:aa"))
      .setVlan(new VlanId(1))
      .build();
    Addresses diffMac = new AddressesBuilder()
      .setIp(new IpAddress("1.1.1.1".toCharArray()))
      .setMac(new MacAddress("aa:aa:aa:aa:aa:ab"))
      .setVlan(new VlanId(1))
      .build();
    Addresses diffVlan = new AddressesBuilder()
      .setIp(new IpAddress("1.1.1.1".toCharArray()))
      .setMac(new MacAddress("aa:aa:aa:aa:aa:aa"))
      .setVlan(new VlanId(2))
      .build();

    assertTrue(Compare.Addresses(a1, a1));
    assertTrue(Compare.Addresses(a1, a2));
    assertFalse(Compare.Addresses(a1, diffIp));
    assertFalse(Compare.Addresses(a1, diffMac));
    assertFalse(Compare.Addresses(a1, diffVlan));
  }

  @Test
  public void testAttachmentPoints() throws Exception {
    //Will break with your new commit
    /*AttachmentPoints atp1 = new AttachmentPointsBuilder()
      .setTpId(new TpId("1"))
      .build();
    AttachmentPoints sameTpId = new AttachmentPointsBuilder()
      .setTpId(new TpId("1"))
      .build();
    AttachmentPoints diffTpId = new AttachmentPointsBuilder()
      .setTpId(new TpId("2"))
      .build();
    AttachmentPoints nullTpId = new AttachmentPointsBuilder()
      .build();

    assertTrue(Compare.AttachmentPoints(atp1, atp1));
    assertTrue(Compare.AttachmentPoints(atp1, sameTpId));
    assertTrue(Compare.AttachmentPoints(nullTpId, nullTpId));
    assertFalse(Compare.AttachmentPoints(atp1, diffTpId));
    assertFalse(Compare.AttachmentPoints(atp1, nullTpId));*/
  }
}
