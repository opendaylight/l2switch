/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AddressObserver listens to IPv4 packets to find addresses (mac, ip) and
 * store these address observations for each node-connector.
 * These packets are returned to the network after the addresses are learned.
 */
public class AddressObserverUsingIpv4 implements Ipv4PacketListener {

  private final static Logger _logger = LoggerFactory.getLogger(AddressObserverUsingIpv4.class);
  private AddressObservationWriter addressObservationWriter;
  private final String IPV4_IP_TO_IGNORE = "0.0.0.0";

  public AddressObserverUsingIpv4(AddressObservationWriter addressObservationWriter) {
    this.addressObservationWriter = addressObservationWriter;
  }

  /**
   * The handler function for IPv4 packets.
   *
   * @param packetReceived The incoming packet.
   */
  @Override
  public void onIpv4PacketReceived(Ipv4PacketReceived packetReceived) {
    if(packetReceived == null || packetReceived.getPacketChain() == null) {
      return;
    }

    RawPacket rawPacket = null;
    EthernetPacket ethernetPacket = null;
    Ipv4Packet ipv4Packet = null;
    for(PacketChain packetChain : packetReceived.getPacketChain()) {
      if(packetChain.getPacket() instanceof RawPacket) {
        rawPacket = (RawPacket) packetChain.getPacket();
      } else if(packetChain.getPacket() instanceof EthernetPacket) {
        ethernetPacket = (EthernetPacket) packetChain.getPacket();
      } else if(packetChain.getPacket() instanceof Ipv4Packet) {
        ipv4Packet = (Ipv4Packet) packetChain.getPacket();
      }
    }
    if(rawPacket == null || ethernetPacket == null || ipv4Packet == null) {
      return;
    }

    if(!IPV4_IP_TO_IGNORE.equals(ipv4Packet.getSourceIpv4().getValue())) {
      addressObservationWriter.addAddress(ethernetPacket.getSourceMac(),
          new IpAddress(ipv4Packet.getSourceIpv4().getValue().toCharArray()),
          rawPacket.getIngress());
    }
  }
}
