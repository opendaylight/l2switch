/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;

/**
 * This class listens to certain type of packets and writes
 * a mac to mac flows.
 */
public class ReactiveFlowWriter implements ArpPacketListener {
  private InventoryReader inventoryReader;
  private FlowWriterService flowWriterService;
  private final MacAddress MAC_TO_IGNORE = new MacAddress("ff:ff:ff:ff:ff:ff");

  public ReactiveFlowWriter(InventoryReader inventoryReader, FlowWriterService flowWriterService) {
    this.inventoryReader = inventoryReader;
    this.flowWriterService = flowWriterService;
  }

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
    MacAddress destMac = ethernetPacket.getDestinationMac();
    if(!MAC_TO_IGNORE.equals(destMac)) {
      writeFlows(packetReceived.getPayload(),
          rawPacket.getIngress(),
          ethernetPacket.getSourceMac(),
          ethernetPacket.getDestinationMac());
    }
  }

  /**
   * Invokes flow writer service to write  bidirectional mac-mac flows on a switch.
   *
   * @param payload The payload to be sent.
   * @param ingress The NodeConnector where the payload came from.
   * @param srcMac  The source MacAddress of the packet.
   * @param destMac The destination MacAddress of the packet.
   */
  public void writeFlows(byte[] payload, NodeConnectorRef ingress, MacAddress srcMac, MacAddress destMac) {
    NodeConnectorRef destNodeConnector = inventoryReader.getNodeConnector(ingress.getValue().firstIdentifierOf(Node.class), destMac);
    if(destNodeConnector != null) {
      flowWriterService.addBidirectionalMacToMacFlows(srcMac, ingress, destMac, destNodeConnector);
    }
  }
}
