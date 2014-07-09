/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addressobserver;

import org.opendaylight.l2switch.packet.PacketDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketOverEthernetReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketOverEthernetReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AddressObserver listens to ARP, IPv4, and IPv6 packets to find addresses (mac, ip) and
 * store these address observations for each node-connector.
 * These packets are returned to the network after the addresses are learned.
 */
public class AddressObserver implements ArpPacketListener, Ipv4PacketListener {

  private final static Logger _logger = LoggerFactory.getLogger(AddressObserver.class);
  private AddressObservationWriter addressObservationWriter;
  private PacketDispatcher packetDispatcher;
  private final String IPV4_IP_TO_IGNORE ="0.0.0.0";

  public AddressObserver(AddressObservationWriter addressObservationWriter, PacketDispatcher packetDispatcher) {
    this.addressObservationWriter = addressObservationWriter;
    this.packetDispatcher = packetDispatcher;
  }

  /**
   * The handler function for ARP packets.
   * @param packetReceived  The incoming packet.
   */
  @Override
  public void onArpPacketOverEthernetReceived(ArpPacketOverEthernetReceived packetReceived) {
    addressObservationWriter.addAddress(packetReceived.getEthernetOverRawPacket().getEthernetPacket().getSourceMac(),
      new IpAddress(packetReceived.getArpPacket().getSourceProtocolAddress().toCharArray()),
      packetReceived.getEthernetOverRawPacket().getRawPacket().getIngress());
    packetDispatcher.sendPacketOut(packetReceived.getPayload(), packetReceived.getEthernetOverRawPacket().getRawPacket().getIngress());
  }

  /**
   * The handler function for IPv4 packets.
   * @param packetReceived  The incoming packet.
   */
  @Override
  public void onIpv4PacketOverEthernetReceived(Ipv4PacketOverEthernetReceived packetReceived) {
    if(!IPV4_IP_TO_IGNORE.equals(packetReceived.getIpv4Packet().getSourceIpv4().getValue())) {
      addressObservationWriter.addAddress(packetReceived.getEthernetOverRawPacket().getEthernetPacket().getSourceMac(),
          new IpAddress(packetReceived.getIpv4Packet().getSourceIpv4().getValue().toCharArray()),
          packetReceived.getEthernetOverRawPacket().getRawPacket().getIngress());
      packetDispatcher.sendPacketOut(packetReceived.getPayload(), packetReceived.getEthernetOverRawPacket().getRawPacket().getIngress());
    }
  }

  // ToDo Ipv6 handler

}
