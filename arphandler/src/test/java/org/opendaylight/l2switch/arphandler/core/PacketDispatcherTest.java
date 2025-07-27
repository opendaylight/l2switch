/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.l2switch.arphandler.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacket;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@ExtendWith(MockitoExtension.class)
class PacketDispatcherTest {
    @Mock
    private TransmitPacket transmitPacket;
    @Mock
    private InventoryReader inventoryReader;
    @Mock
    private HashMap<String, NodeConnectorRef> controllerSwitchConnectors;
    @Mock
    private HashMap<String, List<NodeConnectorRef>> switchNodeConnectors;

    private PacketDispatcher packetDispatcher;

    @BeforeEach
    void beforeEach() {
        packetDispatcher = new PacketDispatcher(inventoryReader, transmitPacket);
    }

    @Test
    void testSendPacketOut() {
        doReturn(RpcResultBuilder.success().buildFuture()).when(transmitPacket).invoke(any());

        final var ncInsId1 = DataObjectIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("abc")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1")))
            .build();
        packetDispatcher.sendPacketOut(null, new NodeConnectorRef(ncInsId1), new NodeConnectorRef(ncInsId1));
        verify(transmitPacket, times(1)).invoke(any());
    }

    @Test
    void testSendPacketOut_NullIngress() {
        packetDispatcher.sendPacketOut(null, null, new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("abc")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("def")))
            .build()));
        verify(transmitPacket, times(0)).invoke(any());
    }

    @Test
    void testSendPacketOut_NullEgress() {
        packetDispatcher.sendPacketOut(null, new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("abc")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("def")))
            .build()), null);
        verify(transmitPacket, times(0)).invoke(any());
    }

    @Test
    void testFloodPacket() {
        doReturn(RpcResultBuilder.success().buildFuture()).when(transmitPacket).invoke(any());

        var nodeConnectors = new ArrayList<NodeConnectorRef>();
        var ncInsId1 = DataObjectIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("abc")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1")))
            .build();
        var ncInsId2 = DataObjectIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("abc")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("2")))
            .build();
        nodeConnectors.add(new NodeConnectorRef(ncInsId1));
        nodeConnectors.add(new NodeConnectorRef(ncInsId1));
        nodeConnectors.add(new NodeConnectorRef(ncInsId2));
        when(switchNodeConnectors.get(any(String.class))).thenReturn(nodeConnectors);
        when(inventoryReader.getSwitchNodeConnectors()).thenReturn(switchNodeConnectors);

        packetDispatcher.floodPacket("", null, new NodeConnectorRef(ncInsId2),
                new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
                    .child(Node.class, new NodeKey(new NodeId("abc")))
                    .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("def")))
                    .build()));
        verify(inventoryReader, times(0)).setRefreshData(true);
        verify(inventoryReader, times(0)).readInventory();
        verify(transmitPacket, times(2)).invoke(any());
    }

    @Test
    void testFloodPacket_NullList() {
        when(switchNodeConnectors.get(any(String.class))).thenReturn(null);
        when(inventoryReader.getSwitchNodeConnectors()).thenReturn(switchNodeConnectors);

        packetDispatcher.floodPacket("", null,
            new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("abc")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("def")))
                .build()),
            new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("abc")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("def")))
                .build()));
        verify(inventoryReader, times(1)).setRefreshData(true);
        verify(inventoryReader, times(1)).readInventory();
        verify(transmitPacket, times(0)).invoke(any());
    }

    @Test
    void testDispatchPacket_noDispatch() {
        var ncInsId = DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        when(controllerSwitchConnectors.get(any(String.class))).thenReturn(null);
        when(inventoryReader.getControllerSwitchConnectors()).thenReturn(controllerSwitchConnectors);

        packetDispatcher.dispatchPacket(null, new NodeConnectorRef(ncInsId), null, null);
        verify(inventoryReader, times(2)).readInventory();
        verify(inventoryReader, times(1)).setRefreshData(true);
        verify(transmitPacket, times(0)).invoke(any());
    }

    @Test
    void testDispatchPacket_toSendPacketOut() {
        doReturn(RpcResultBuilder.success().buildFuture()).when(transmitPacket).invoke(any());

        var ncInsId1 = DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        var ncRef1 = new NodeConnectorRef(ncInsId1);
        when(controllerSwitchConnectors.get(any(String.class))).thenReturn(ncRef1);
        when(inventoryReader.getControllerSwitchConnectors()).thenReturn(controllerSwitchConnectors);
        when(inventoryReader.getNodeConnector(any(), any())).thenReturn(ncRef1);

        packetDispatcher.dispatchPacket(null, new NodeConnectorRef(ncInsId1), null, null);
        verify(inventoryReader, times(1)).readInventory();
        verify(inventoryReader, times(0)).setRefreshData(true);
        verify(transmitPacket, times(1)).invoke(any());
    }

    @Test
    void testDispatchPacket_toFloodPacket() {
        doReturn(RpcResultBuilder.success().buildFuture()).when(transmitPacket).invoke(any());

        var ncInsId1 = DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        var ncRef1 = new NodeConnectorRef(ncInsId1);
        when(controllerSwitchConnectors.get(any(String.class))).thenReturn(ncRef1);
        when(inventoryReader.getControllerSwitchConnectors()).thenReturn(controllerSwitchConnectors);
        when(inventoryReader.getNodeConnector(any(), any())).thenReturn(null);

        var nodeConnectors = new ArrayList<NodeConnectorRef>();
        var ncInsId2 = DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("2")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("2"))).build();
        nodeConnectors.add(new NodeConnectorRef(ncInsId1));
        nodeConnectors.add(new NodeConnectorRef(ncInsId1));
        nodeConnectors.add(new NodeConnectorRef(ncInsId2));
        when(switchNodeConnectors.get(any(String.class))).thenReturn(nodeConnectors);
        when(inventoryReader.getSwitchNodeConnectors()).thenReturn(switchNodeConnectors);

        packetDispatcher.dispatchPacket(null, new NodeConnectorRef(ncInsId2), null, null);
        verify(inventoryReader, times(1)).readInventory();
        verify(inventoryReader, times(0)).setRefreshData(true);
        verify(transmitPacket, times(2)).invoke(any());
    }
}
