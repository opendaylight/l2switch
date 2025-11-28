/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.raw.packet.RawPacketFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@ExtendWith(MockitoExtension.class)
class ReactiveFlowWriterTest {
    @Mock
    private InventoryReader inventoryReader;
    @Mock
    private FlowWriterService flowWriterService;
    @Mock
    private NodeConnectorRef destNodeConnectorRef;

    private ReactiveFlowWriter reactiveFlowWriter;
    private DataObjectIdentifier<Node> nodeInstanceIdentifier;
    private NodeConnectorRef nodeConnectorRef;

    @BeforeEach
    void beforeEach() {
        reactiveFlowWriter = new ReactiveFlowWriter(inventoryReader, flowWriterService);
        nodeInstanceIdentifier = DataObjectIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("abc")))
            .build();
        nodeConnectorRef = new NodeConnectorRef(nodeInstanceIdentifier);
    }

    @Test
    void onArpPacketReceivedTest() {
        reactiveFlowWriter.onNotification(new ArpPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder()
                    .setPacket(new RawPacketBuilder()
                        .setRawPacketFields(new RawPacketFieldsBuilder().setIngress(nodeConnectorRef).build())
                        .build())
                    .build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("00:00:00:00:00:01")).build())
                    .build(),
                new PacketChainBuilder()
                    .setPacket(new ArpPacketBuilder().setSourceProtocolAddress("10.0.0.1").build())
                    .build()))
            .build());
    }

    @Test
    void writeFlowsTest() {
        when(inventoryReader.getNodeConnector(any(InstanceIdentifier.class), any(MacAddress.class)))
            .thenReturn(destNodeConnectorRef);
        reactiveFlowWriter.writeFlows(nodeConnectorRef, new MacAddress("00:00:00:00:00:01"),
                new MacAddress("00:00:00:00:00:02"));

        verify(inventoryReader, times(1)).getNodeConnector(any(InstanceIdentifier.class), any(MacAddress.class));
        verify(flowWriterService, times(1)).addBidirectionalMacToMacFlows(any(MacAddress.class),
                any(NodeConnectorRef.class), any(MacAddress.class), any(NodeConnectorRef.class));
    }
}

