/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.arphandler.core;

import org.opendaylight.l2switch.arphandler.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * PacketDispatcher sends packets out to the network.
 */
public class PacketDispatcher {

  private final static Logger _logger = LoggerFactory.getLogger(PacketDispatcher.class);
  private InventoryReader inventoryReader;
  private PacketProcessingService packetProcessingService;

  public void setPacketProcessingService(PacketProcessingService packetProcessingService) {
    this.packetProcessingService = packetProcessingService;
  }

  public void setInventoryReader(InventoryReader inventoryReader) {
    this.inventoryReader = inventoryReader;
  }

  /**
   * Dispatches the packet in the appropriate way - flood or unicast.
   *
   * @param payload The payload to be sent.
   * @param ingress The NodeConnector where the payload came from.
   * @param srcMac  The source MacAddress of the packet.
   * @param destMac The destination MacAddress of the packet.
   */
  public void dispatchPacket(byte[] payload, NodeConnectorRef ingress, MacAddress srcMac, MacAddress destMac) {
    inventoryReader.readInventory();

    String nodeId = ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class).getId().getValue();
    NodeConnectorRef srcConnectorRef = inventoryReader.getControllerSwitchConnectors().get(nodeId);

    if(srcConnectorRef == null) {
      refreshInventoryReader();
      srcConnectorRef = inventoryReader.getControllerSwitchConnectors().get(nodeId);
    }
    NodeConnectorRef destNodeConnector = inventoryReader.getNodeConnector(ingress.getValue().firstIdentifierOf(Node.class), destMac);
    if(srcConnectorRef != null) {
      if(destNodeConnector != null) {
        sendPacketOut(payload, srcConnectorRef, destNodeConnector);
      } else {
        floodPacket(nodeId, payload, ingress, srcConnectorRef);
      }
    } else {
      _logger.info("Cannot send packet out or flood as controller node connector is not available for node {}.", nodeId);
    }
  }

  /**
   * Floods the packet.
   *
   * @param payload     The payload to be sent.
   * @param origIngress The NodeConnector where the payload came from.
   */
  public void floodPacket(String nodeId, byte[] payload, NodeConnectorRef origIngress, NodeConnectorRef controllerNodeConnector) {

    List<NodeConnectorRef> nodeConnectors = inventoryReader.getSwitchNodeConnectors().get(nodeId);

    if(nodeConnectors == null) {
      refreshInventoryReader();
      nodeConnectors = inventoryReader.getSwitchNodeConnectors().get(nodeId);
      if(nodeConnectors == null) {
        _logger.info("Cannot flood packets, as inventory doesn't have any node connectors for node {}", nodeId);
        return;
      }
    }
    for(NodeConnectorRef ncRef : nodeConnectors) {
      String ncId = ncRef.getValue().firstIdentifierOf(NodeConnector.class).firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue();
      // Don't flood on discarding node connectors & origIngress
      if(!ncId.equals(origIngress.getValue().firstIdentifierOf(NodeConnector.class).firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue())) {
        sendPacketOut(payload, controllerNodeConnector, ncRef);
      }
    }
  }

  /**
   * Sends the specified packet on the specified port.
   *
   * @param payload The payload to be sent.
   * @param ingress The NodeConnector where the payload came from.
   * @param egress  The NodeConnector where the payload will go.
   */
  public void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
    if(ingress == null || egress == null) return;
    InstanceIdentifier<Node> egressNodePath = getNodePath(egress.getValue());
    TransmitPacketInput input = new TransmitPacketInputBuilder() //
        .setPayload(payload) //
        .setNode(new NodeRef(egressNodePath)) //
        .setEgress(egress) //
        .setIngress(ingress) //
        .build();
    packetProcessingService.transmitPacket(input);
  }

  private void refreshInventoryReader() {
    inventoryReader.setRefreshData(true);
    inventoryReader.readInventory();
  }

  private InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
    return nodeChild.firstIdentifierOf(Node.class);
  }

}
