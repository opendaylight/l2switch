/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.packet;

import org.opendaylight.l2switch.flow.FlowWriterService;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.l2switch.util.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * PacketDispatcher sends packets out to the network.
 */
public class PacketDispatcher  {

  private final static Logger _logger = LoggerFactory.getLogger(PacketDispatcher.class);
  private InventoryReader inventoryReader;
  private PacketProcessingService packetProcessingService;
  private FlowWriterService flowWriterService;

  public void setPacketProcessingService(PacketProcessingService packetProcessingService) {
    this.packetProcessingService = packetProcessingService;
  }

  public void setFlowWriterService(FlowWriterService flowWriterService) {
    this.flowWriterService = flowWriterService;
  }

  public void setInventoryReader(InventoryReader inventoryReader) {
    this.inventoryReader = inventoryReader;
  }

  /**
   * Dispatches the packet in the appropriate way - flood or unicast.
   * @param payload The payload to be sent.
   * @param ingress The NodeConnector where the payload came from.
   * @param srcMac The source MacAddress of the packet.
   * @param destMac The destination MacAddress of the packet.
   */
  public void dispatchPacket(byte[] payload, NodeConnectorRef ingress, MacAddress srcMac, MacAddress destMac) {
    inventoryReader.readInventory();
    NodeConnectorRef destNodeConnector = inventoryReader.getNodeConnector(ingress.getValue().firstIdentifierOf(Node.class), destMac);
    if (destNodeConnector != null) {
      flowWriterService.addBidirectionalMacToMacFlows(srcMac, ingress, destMac, destNodeConnector);
      String nodeId = ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class).getId().getValue();
      sendPacketOut(payload, inventoryReader.getControllerSwitchConnectors().get(nodeId), destNodeConnector);
    }
    else {
      floodPacket(payload, ingress);
    }
  }

  /**
   * Floods the packet.
   * @param payload The payload to be sent.
   * @param ingress The NodeConnector where the payload came from.
   */
  public void floodPacket(byte[] payload, NodeConnectorRef ingress) {
    // Get the name of the node
    String nodeId = "";
    InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.getNodePath(ingress.getValue());
    if (nodePath != null) {
      NodeKey nodeKey = InstanceIdentifierUtils.getNodeKey(nodePath);
      if (nodeKey != null) {
        nodeId = nodeKey.getId().getValue();
      }
    }

    // Flood this packet from the original node
    NodeConnectorRef pktIngress = inventoryReader.getControllerSwitchConnectors().get(nodeId);
    List<NodeConnectorRef> nodeConnectors = inventoryReader.getSwitchNodeConnectors().get(nodeId);
    for (NodeConnectorRef ncRef : nodeConnectors) {
      String ncId = ncRef.getValue().firstIdentifierOf(NodeConnector.class).firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue();
      // Don't flood on discarding node connectors & ingress
      if (!inventoryReader.getDiscardingNodeConnectors().contains(ncId) &&
        !ncId.equals(ingress.getValue().firstIdentifierOf(NodeConnector.class).firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue())) {
        sendPacketOut(payload, pktIngress, ncRef);
      }
    }
  }

  /**
   * Sends the specified packet on the specified port.
   * @param payload  The payload to be sent.
   * @param ingress  The NodeConnector where the payload came from.
   * @param egress  The NodeConnector where the payload will go.
   */
  public void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
    if (ingress == null || egress == null)  return;
    InstanceIdentifier<Node> egressNodePath = InstanceIdentifierUtils.getNodePath(egress.getValue());
    TransmitPacketInput input = new TransmitPacketInputBuilder() //
      .setPayload(payload) //
      .setNode(new NodeRef(egressNodePath)) //
      .setEgress(egress) //
      .setIngress(ingress) //
      .build();
    packetProcessingService.transmitPacket(input);
  }
}
