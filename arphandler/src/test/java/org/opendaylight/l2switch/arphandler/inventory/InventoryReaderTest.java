/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class InventoryReaderTest {
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readOnlyTransaction;
    @Mock
    private InstanceIdentifier<Node> mockInstanceIdentifier;
    @Mock
    private MacAddress mockMacAddress;

    private InventoryReader inventoryReader;

    @Before
    public void before() {
        inventoryReader = new InventoryReader(dataBroker);
    }

    @Test
    public void testGetControllerSwitchConnectors() throws Exception {
        assertEquals(Map.of(), inventoryReader.getControllerSwitchConnectors());
    }

    @Test
    public void testGetSwitchNodeConnectors() throws Exception {
        assertEquals(Map.of(), inventoryReader.getSwitchNodeConnectors());
    }

    @Test
    public void testGetNodeConnector() throws Exception {
        Node node = new NodeBuilder()
            .setId(new NodeId("nodeId"))
            .setNodeConnector(BindingMap.of(new NodeConnectorBuilder()
                .setId(new NodeConnectorId("connId"))
                .addAugmentation(new StpStatusAwareNodeConnectorBuilder().setStatus(StpStatus.Forwarding).build())
                .addAugmentation(new AddressCapableNodeConnectorBuilder()
                    .setAddresses(BindingMap.of(new AddressesBuilder()
                        .setId(Uint64.ZERO)
                        .setLastSeen(0L)
                        .setMac(new MacAddress("aa:bb:cc:dd:ee:ff"))
                        .build()))
                    .build())
                .build()))
            .build();

        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(DataObjectIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(node)));
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        assertNotNull(inventoryReader.getNodeConnector(
            InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId("openflow:1"))).build(),
            new MacAddress("aa:bb:cc:dd:ee:ff")));
        verify(readOnlyTransaction, times(1)).close();
    }

    @Test
    public void testGetNodeConnector_NullNodeInsId() throws Exception {
        assertNull(inventoryReader.getNodeConnector(null, mockMacAddress));
        verify(dataBroker, times(0)).newReadOnlyTransaction();
    }

    @Test
    public void testGetNodeConnector_NullMacAddress() throws Exception {
        assertNull(inventoryReader.getNodeConnector(mockInstanceIdentifier, null));
        verify(dataBroker, times(0)).newReadOnlyTransaction();
    }

    @Test
    public void testReadInventory_NoRefresh() throws Exception {
        inventoryReader.setRefreshData(false);
        inventoryReader.readInventory();
        verify(dataBroker, times(0)).newReadOnlyTransaction();
    }

    @Test
    public void testReadInventory_Refresh() throws Exception {
        StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder()
                .setStatus(StpStatus.Discarding).build();

        Nodes nodes = new NodesBuilder()
            .setNode(BindingMap.of(new NodeBuilder()
                .setId(new NodeId("1"))
                .setNodeConnector(BindingMap.of(
                    new NodeConnectorBuilder().setId(new NodeConnectorId("1")).build(),
                    new NodeConnectorBuilder().setId(new NodeConnectorId("2")).build(),
                    new NodeConnectorBuilder()
                        .setId(new NodeConnectorId("3"))
                        .addAugmentation(stpStatusAwareNodeConnector)
                        .build(),
                    new NodeConnectorBuilder()
                        .setId(new NodeConnectorId("LOCAL"))
                        .addAugmentation(stpStatusAwareNodeConnector)
                        .build()))
                .build()))
            .build();

        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(DataObjectIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(nodes)));
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        inventoryReader.setRefreshData(true);
        inventoryReader.readInventory();
        verify(dataBroker, times(1)).newReadOnlyTransaction();
        assertEquals(1, inventoryReader.getControllerSwitchConnectors().size());
        assertEquals(1, inventoryReader.getSwitchNodeConnectors().size());
        assertEquals(2, inventoryReader.getSwitchNodeConnectors().get("1").size());
        // Ensure that refreshData is set to false
        inventoryReader.readInventory();
        verify(dataBroker, times(1)).newReadOnlyTransaction();
    }
}
