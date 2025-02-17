/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public class ProactiveFloodFlowWriterTest {
    @Mock
    private DataBroker dataBroker;
    @Mock
    private AddFlow addFlow;
    @Mock
    private ReadTransaction readOnlyTransaction;

    private ProactiveFloodFlowWriter proactiveFloodFlowWriter;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        proactiveFloodFlowWriter = new ProactiveFloodFlowWriter(dataBroker, addFlow);
    }

    @Test
    public void testSetFlowInstallationDelay() throws Exception {
        proactiveFloodFlowWriter.setFlowInstallationDelay(0);
    }

    @Test
    public void testSetFlowTableId() throws Exception {
        proactiveFloodFlowWriter.setFlowTableId(Uint8.ZERO);
    }

    @Test
    public void testSetFlowPriority() throws Exception {
        proactiveFloodFlowWriter.setFlowPriority(Uint16.ZERO);
    }

    @Test
    public void testSetFlowIdleTimeout() throws Exception {
        proactiveFloodFlowWriter.setFlowIdleTimeout(Uint16.ZERO);
    }

    @Test
    public void testSetFlowHardTimeout() throws Exception {
        proactiveFloodFlowWriter.setFlowHardTimeout(Uint16.ZERO);
    }

    @Test
    public void testRegisterAsDataChangeListener() throws Exception {
        proactiveFloodFlowWriter.registerAsDataChangeListener();
        verify(dataBroker, times(1)).registerDataListener(any(), any());
    }

    @Test
    public void testOnDataChanged_CreatedDataRefresh() throws Exception {
        StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder()
                .setStatus(StpStatus.Discarding).build();

        Nodes nodes = new NodesBuilder()
            .setNode(BindingMap.of(new NodeBuilder()
                .setId(new NodeId("nodeId"))
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

        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(nodes)));
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        proactiveFloodFlowWriter.setFlowInstallationDelay(0);
        proactiveFloodFlowWriter.dataChangedTo(null);
        verify(dataBroker, after(250)).newReadOnlyTransaction();
        verify(addFlow, times(2)).invoke(any(AddFlowInput.class));
    }
}
