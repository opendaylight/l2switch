/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class ReactiveFlowWriterTest {

    @MockitoAnnotations.Mock private InventoryReader inventoryReader;
    @MockitoAnnotations.Mock private FlowWriterService flowWriterService;
    @MockitoAnnotations.Mock private NodeConnectorRef destNodeConnectorRef;
    private ReactiveFlowWriter reactiveFlowWriter;
    private InstanceIdentifier<Node> nodeInstanceIdentifier;
    private NodeConnectorRef nodeConnectorRef;
    private ArrayList<PacketChain> packetChainList;



    @Before
    public void initMocks() {

        MockitoAnnotations.initMocks(this);
        reactiveFlowWriter = new ReactiveFlowWriter(inventoryReader, flowWriterService);

        nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class).toInstance();
        nodeConnectorRef = new NodeConnectorRef(nodeInstanceIdentifier);
        packetChainList = new ArrayList<PacketChain>();
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new RawPacketBuilder().setIngress(nodeConnectorRef).build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("00:00:00:00:00:01")).build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new ArpPacketBuilder().setSourceProtocolAddress("10.0.0.1").build())
            .build());
    }

    @Test
    public void onArpPacketReceivedTest() {

        ArpPacketReceived arpPacketReceived = new ArpPacketReceivedBuilder().setPacketChain(packetChainList).build();
        reactiveFlowWriter.onArpPacketReceived(arpPacketReceived);

    }

    @Test
    public void writeFlowsTest() {

        ArpPacketReceived arpPacketReceived = new ArpPacketReceivedBuilder().setPacketChain(packetChainList).build();
        when(inventoryReader.getNodeConnector(any(InstanceIdentifier.class), any(MacAddress.class))).thenReturn(destNodeConnectorRef);
        reactiveFlowWriter.writeFlows(arpPacketReceived.getPayload(), nodeConnectorRef, new MacAddress("00:00:00:00:00:01"), new MacAddress("00:00:00:00:00:02"));

        verify(inventoryReader, times(1)).getNodeConnector(any(InstanceIdentifier.class), any(MacAddress.class));
        verify(flowWriterService, times(1)).addBidirectionalMacToMacFlows(any(MacAddress.class), any(NodeConnectorRef.class), any(MacAddress.class), any(NodeConnectorRef.class));

    }
}

