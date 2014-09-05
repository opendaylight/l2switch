/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AddressObserver listens to ARP packets to find addresses (mac, ip) and
 * store these address observations for each node-connector.
 * These packets are returned to the network after the addresses are learned.
 */
public class AddressObserverUsingArp implements ArpPacketListener {

  private final static Logger _logger = LoggerFactory.getLogger(AddressObserverUsingArp.class);
  private org.opendaylight.l2switch.addresstracker.addressobserver.AddressObservationWriter addressObservationWriter;

  public AddressObserverUsingArp(org.opendaylight.l2switch.addresstracker.addressobserver.AddressObservationWriter addressObservationWriter) {
    this.addressObservationWriter = addressObservationWriter;
  }

  /**
   * The handler function for ARP packets.
   *
   * @param packetReceived The incoming packet.
   */
  @Override
  public void onArpPacketReceived(ArpPacketReceived packetReceived) {
    if(packetReceived == null || packetReceived.getPacketChain() == null) {
      return;
    }

    RawPacket rawPacket = null;
    EthernetPacket ethernetPacket = null;
    ArpPacket arpPacket = null;
    for(PacketChain packetChain : packetReceived.getPacketChain()) {
      if(packetChain.getPacket() instanceof RawPacket) {
        rawPacket = (RawPacket) packetChain.getPacket();
      } else if(packetChain.getPacket() instanceof EthernetPacket) {
        ethernetPacket = (EthernetPacket) packetChain.getPacket();
      } else if(packetChain.getPacket() instanceof ArpPacket) {
        arpPacket = (ArpPacket) packetChain.getPacket();
      }
    }
    if(rawPacket == null || ethernetPacket == null || arpPacket == null) {
      return;
    }

    addressObservationWriter.addAddress(ethernetPacket.getSourceMac(),
        new IpAddress(arpPacket.getSourceProtocolAddress().toCharArray()),
        rawPacket.getIngress());
  }
}
