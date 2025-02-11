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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InitialFlowWriterTest {
    @Mock
    private AddFlow addFlow;
    @Mock
    private DataTreeModification<Node> mockChange;
    @Mock
    private DataObjectModification<Node> mockModification;
    private InitialFlowWriter initialFlowWriter;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        initialFlowWriter = new InitialFlowWriter(addFlow);
    }

    @Test
    public void onDataChange_Valid() throws Exception {
        InstanceIdentifier<Node> instanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                .build();
        Node topoNode = new NodeBuilder().setId(new NodeId("openflow:1")).build();
        when(mockModification.getDataAfter()).thenReturn(topoNode);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(mockChange.getRootPath()).thenReturn(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);

        initialFlowWriter.onDataTreeChanged(List.of(mockChange));
        Thread.sleep(250);
        verify(addFlow, times(1)).invoke(any(AddFlowInput.class));
    }
}
