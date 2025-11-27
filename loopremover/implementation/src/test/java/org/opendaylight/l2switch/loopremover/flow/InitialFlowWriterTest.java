/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@ExtendWith(MockitoExtension.class)
class InitialFlowWriterTest {
    @Mock
    private AddFlow addFlow;
    @Mock
    private DataTreeModification<Node> mockChange;
    @Mock
    private DataObjectModification<Node> mockModification;
    private InitialFlowWriter initialFlowWriter;

    @BeforeEach
    void beforeEach() {
        initialFlowWriter = new InitialFlowWriter(addFlow);
    }

    @Test
    void onDataChange_Valid() {
        var instanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                .build();

        when(mockModification.dataBefore()).thenReturn(null);
        when(mockModification.modificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(mockChange.getRootPath()).thenReturn(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);

        initialFlowWriter.onDataTreeChanged(List.of(mockChange));
        verify(addFlow, timeout(250)).invoke(any());
    }
}
