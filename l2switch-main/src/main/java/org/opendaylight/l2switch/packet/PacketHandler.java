/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.packet;

import org.opendaylight.l2switch.addresstracker.AddressTracker;
import org.opendaylight.l2switch.flow.FlowWriterService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketOverEthernetReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketOverEthernetReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PacketListener listens to ARP, IPv4, and IPv6 packets to find addresses (mac, ip) and
 * store these address observations for each node-connector.
 * These packets are returned to the network after the addresses are learned.
 */
public class PacketHandler implements ArpPacketListener, Ipv4PacketListener {

  private final static Logger _logger = LoggerFactory.getLogger(PacketHandler.class);
  private AddressTracker addressTracker;
  private PacketProcessingService packetProcessingService;
  private FlowWriterService flowWriterService;

  public void setAddressTracker(AddressTracker addressTracker) {
    this.addressTracker = addressTracker;
  }

  public void setPacketProcessingService(PacketProcessingService packetProcessingService) {
    this.packetProcessingService = packetProcessingService;
  }

  public void setFlowWriterService(FlowWriterService flowWriterService) {
    this.flowWriterService = flowWriterService;
  }

  /**
   * The handler function for ARP packets.
   * @param packetReceived  The incoming packet.
   */
  @Override
  public void onArpPacketOverEthernetReceived(ArpPacketOverEthernetReceived packetReceived) {
    System.out.println(packetReceived);
    addressTracker.addAddress(packetReceived.getEthernetOverRawPacket().getEthernetPacket().getSourceMac(),
      new IpAddress(packetReceived.getArpPacket().getSourceProtocolAddress().toCharArray()),
      packetReceived.getEthernetOverRawPacket().getRawPacket().getIngress());
    //SendPacketOut
  }

  /**
   * The handler function for IPv4 packets.
   * @param packetReceived  The incoming packet.
   */
  @Override
  public void onIpv4PacketOverEthernetReceived(Ipv4PacketOverEthernetReceived packetReceived) {
    addressTracker.addAddress(packetReceived.getEthernetOverRawPacket().getEthernetPacket().getSourceMac(),
      new IpAddress(packetReceived.getIpv4Packet().getSourceIpv4()),
      packetReceived.getEthernetOverRawPacket().getRawPacket().getIngress());
    // sendPacketOut
  }

  // ToDo Ipv6 handler

  /**
   * Sends the specified packet on the specified port.
   * @param payload  The payload to be sent.
   * @param ingress  The NodeConnector where the payload came from.
   * @param egress  The NodeConnector where the payload will go.
   */
  private void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
    if (ingress == null || egress == null)  return;
    /*InstanceIdentifier<Node> egressNodePath = InstanceIdentifierUtils.getNodePath(egress.getValue());
    TransmitPacketInput input = new TransmitPacketInputBuilder() //
      .setPayload(payload) //
      .setNode(new NodeRef(egressNodePath)) //
      .setEgress(egress) //
      .setIngress(ingress) //
      .build();
    packetProcessingService.transmitPacket(input);*/
  }

}
