/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.l2switch.arphandler.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class PacketDispatcherTest {
    @Mock
    private PacketProcessingService packetProcessingService;
    @Mock
    private InventoryReader inventoryReader;
    private PacketDispatcher packetDispatcher;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        packetDispatcher = new PacketDispatcher();
        packetDispatcher.setPacketProcessingService(packetProcessingService);
        packetDispatcher.setInventoryReader(inventoryReader);

        doReturn(RpcResultBuilder.success().buildFuture()).when(packetProcessingService).transmitPacket(any());
    }

    @Test
    public void testSendPacketOut() throws Exception {
        InstanceIdentifier<NodeConnector> ncInsId1 = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        packetDispatcher.sendPacketOut(null, new NodeConnectorRef(ncInsId1), new NodeConnectorRef(ncInsId1));
        verify(packetProcessingService, times(1)).transmitPacket(any(TransmitPacketInput.class));
    }

    @Test
    public void testSendPacketOut_NullIngress() throws Exception {
        packetDispatcher.sendPacketOut(null, null,
                new NodeConnectorRef(InstanceIdentifier.create(NodeConnector.class)));
        verify(packetProcessingService, times(0)).transmitPacket(any(TransmitPacketInput.class));
    }

    @Test
    public void testSendPacketOut_NullEgress() throws Exception {
        packetDispatcher.sendPacketOut(null, new NodeConnectorRef(InstanceIdentifier.create(NodeConnector.class)),
                null);
        verify(packetProcessingService, times(0)).transmitPacket(any(TransmitPacketInput.class));
    }

    @Test
    public void testFloodPacket() throws Exception {
        List<NodeConnectorRef> nodeConnectors = new ArrayList<>();
        InstanceIdentifier<NodeConnector> ncInsId1 = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        InstanceIdentifier<NodeConnector> ncInsId2 = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("2"))).build();
        nodeConnectors.add(new NodeConnectorRef(ncInsId1));
        nodeConnectors.add(new NodeConnectorRef(ncInsId1));
        nodeConnectors.add(new NodeConnectorRef(ncInsId2));
        HashMap<String, List<NodeConnectorRef>> switchNodeConnectors = Mockito.mock(HashMap.class);
        when(switchNodeConnectors.get(any(String.class))).thenReturn(nodeConnectors);
        when(inventoryReader.getSwitchNodeConnectors()).thenReturn(switchNodeConnectors);

        packetDispatcher.floodPacket("", null, new NodeConnectorRef(ncInsId2),
                new NodeConnectorRef(InstanceIdentifier.create(NodeConnector.class)));
        verify(inventoryReader, times(0)).setRefreshData(true);
        verify(inventoryReader, times(0)).readInventory();
        verify(packetProcessingService, times(2)).transmitPacket(any(TransmitPacketInput.class));
    }

    @Test
    public void testFloodPacket_NullList() throws Exception {
        HashMap<String, List<NodeConnectorRef>> switchNodeConnectors = Mockito.mock(HashMap.class);
        when(switchNodeConnectors.get(any(String.class))).thenReturn(null);
        when(inventoryReader.getSwitchNodeConnectors()).thenReturn(switchNodeConnectors);

        packetDispatcher.floodPacket("", null, new NodeConnectorRef(InstanceIdentifier.create(NodeConnector.class)),
                new NodeConnectorRef(InstanceIdentifier.create(NodeConnector.class)));
        verify(inventoryReader, times(1)).setRefreshData(true);
        verify(inventoryReader, times(1)).readInventory();
        verify(packetProcessingService, times(0)).transmitPacket(any(TransmitPacketInput.class));
    }

    @Test
    public void testDispatchPacket_noDispatch() throws Exception {
        InstanceIdentifier<NodeConnector> ncInsId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        HashMap<String, NodeConnectorRef> controllerSwitchConnectors = Mockito.mock(HashMap.class);
        when(controllerSwitchConnectors.get(any(String.class))).thenReturn(null);
        when(inventoryReader.getControllerSwitchConnectors()).thenReturn(controllerSwitchConnectors);

        packetDispatcher.dispatchPacket(null, new NodeConnectorRef(ncInsId), null, null);
        verify(inventoryReader, times(2)).readInventory();
        verify(inventoryReader, times(1)).setRefreshData(true);
        verify(packetProcessingService, times(0)).transmitPacket(any(TransmitPacketInput.class));
    }

    @Test
    public void testDispatchPacket_toSendPacketOut() throws Exception {
        InstanceIdentifier<NodeConnector> ncInsId1 = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        NodeConnectorRef ncRef1 = new NodeConnectorRef(ncInsId1);
        HashMap<String, NodeConnectorRef> controllerSwitchConnectors = Mockito.mock(HashMap.class);
        when(controllerSwitchConnectors.get(any(String.class))).thenReturn(ncRef1);
        when(inventoryReader.getControllerSwitchConnectors()).thenReturn(controllerSwitchConnectors);
        when(inventoryReader.getNodeConnector(any(InstanceIdentifier.class), any(MacAddress.class))).thenReturn(ncRef1);

        packetDispatcher.dispatchPacket(null, new NodeConnectorRef(ncInsId1), null, null);
        verify(inventoryReader, times(1)).readInventory();
        verify(inventoryReader, times(0)).setRefreshData(true);
        verify(packetProcessingService, times(1)).transmitPacket(any(TransmitPacketInput.class));
    }

    @Test
    public void testDispatchPacket_toFloodPacket() throws Exception {
        InstanceIdentifier<NodeConnector> ncInsId1 = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        NodeConnectorRef ncRef1 = new NodeConnectorRef(ncInsId1);
        HashMap<String, NodeConnectorRef> controllerSwitchConnectors = Mockito.mock(HashMap.class);
        when(controllerSwitchConnectors.get(any(String.class))).thenReturn(ncRef1);
        when(inventoryReader.getControllerSwitchConnectors()).thenReturn(controllerSwitchConnectors);
        when(inventoryReader.getNodeConnector(any(InstanceIdentifier.class), any(MacAddress.class))).thenReturn(null);

        List<NodeConnectorRef> nodeConnectors = new ArrayList<>();
        InstanceIdentifier<NodeConnector> ncInsId2 = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("2")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("2"))).build();
        nodeConnectors.add(new NodeConnectorRef(ncInsId1));
        nodeConnectors.add(new NodeConnectorRef(ncInsId1));
        nodeConnectors.add(new NodeConnectorRef(ncInsId2));
        HashMap<String, List<NodeConnectorRef>> switchNodeConnectors = Mockito.mock(HashMap.class);
        when(switchNodeConnectors.get(any(String.class))).thenReturn(nodeConnectors);
        when(inventoryReader.getSwitchNodeConnectors()).thenReturn(switchNodeConnectors);

        packetDispatcher.dispatchPacket(null, new NodeConnectorRef(ncInsId2), null, null);
        verify(inventoryReader, times(1)).readInventory();
        verify(inventoryReader, times(0)).setRefreshData(true);
        verify(packetProcessingService, times(2)).transmitPacket(any(TransmitPacketInput.class));
    }
}
