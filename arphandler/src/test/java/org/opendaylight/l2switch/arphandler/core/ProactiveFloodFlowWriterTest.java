/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.arphandler.core;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ProactiveFloodFlowWriterTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private SalFlowService salFlowService;
    private ProactiveFloodFlowWriter proactiveFloodFlowWriter;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        proactiveFloodFlowWriter = new ProactiveFloodFlowWriter(dataBroker, salFlowService);
    }

    @Test
    public void testSetFlowInstallationDelay() throws Exception {
        proactiveFloodFlowWriter.setFlowInstallationDelay(0);
    }

    @Test
    public void testSetFlowTableId() throws Exception {
        proactiveFloodFlowWriter.setFlowTableId((short) 0);
    }

    @Test
    public void testSetFlowPriority() throws Exception {
        proactiveFloodFlowWriter.setFlowPriority(0);
    }

    @Test
    public void testSetFlowIdleTimeout() throws Exception {
        proactiveFloodFlowWriter.setFlowIdleTimeout(0);
    }

    @Test
    public void testSetFlowHardTimeout() throws Exception {
        proactiveFloodFlowWriter.setFlowHardTimeout(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterAsDataChangeListener() throws Exception {
        proactiveFloodFlowWriter.registerAsDataChangeListener();
        verify(dataBroker, times(1)).registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(DataTreeChangeListener.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_CreatedDataRefresh() throws Exception {
        DataTreeModification<StpStatusAwareNodeConnector> mockChange = Mockito.mock(DataTreeModification.class);
        DataObjectModification<StpStatusAwareNodeConnector> mockModification =
                Mockito.mock(DataObjectModification.class);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(mockChange.getRootNode()).thenReturn(mockModification);

        StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder()
                .setStatus(StpStatus.Discarding).build();
        NodeConnector nc1 = new NodeConnectorBuilder().withKey(new NodeConnectorKey(new NodeConnectorId("1"))).build();
        NodeConnector nc2 = new NodeConnectorBuilder().withKey(new NodeConnectorKey(new NodeConnectorId("2"))).build();
        NodeConnector nc3 = new NodeConnectorBuilder().withKey(new NodeConnectorKey(new NodeConnectorId("3")))
                .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector).build();
        NodeConnector ncLocal = new NodeConnectorBuilder().withKey(new NodeConnectorKey(new NodeConnectorId("LOCAL")))
                .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector).build();

        List<NodeConnector> nodeConnectors = new ArrayList<>();
        nodeConnectors.add(nc1);
        nodeConnectors.add(nc2);
        nodeConnectors.add(nc3);
        nodeConnectors.add(ncLocal);
        Node node = new NodeBuilder().setNodeConnector(nodeConnectors).build();

        List<Node> nodeList = new ArrayList<>();
        nodeList.add(node);
        Nodes nodes = new NodesBuilder().setNode(nodeList).build();
        Optional<Nodes> optionalNodes = Optional.of(nodes);

        ReadTransaction readOnlyTransaction = Mockito.mock(ReadTransaction.class);
        FluentFuture checkedFuture = Mockito.mock(FluentFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        proactiveFloodFlowWriter.setFlowInstallationDelay(0);
        proactiveFloodFlowWriter.onDataTreeChanged(Collections.singletonList(mockChange));
        Thread.sleep(250);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
        verify(salFlowService, times(0)).addFlow(any(AddFlowInput.class));
    }
}
