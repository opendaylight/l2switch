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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class InitialFlowWriterTest {
  private SalFlowService salFlowService;
  private NodeUpdated nodeUpdated;
  private AddFlowInput addFlowInput;

  @Before
  public void init() {
    salFlowService = mock(SalFlowService.class);
    nodeUpdated = mock(NodeUpdated.class);
    addFlowInput = mock(AddFlowInput.class);
    NodeRef nodeRef = new NodeRef(getNodeId(new NodeId("openflow:1")));
    when(nodeUpdated.getNodeRef()).thenReturn(nodeRef);
  }

  @Test
  public void testOnNodeUpdated() throws Exception {
    InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
    initialFlowWriter.onNodeUpdated(nodeUpdated);

    verify(salFlowService,times(3));

  }

  private InstanceIdentifier<Node> getNodeId(final NodeId nodeId) {
    return InstanceIdentifier.builder(Nodes.class) //
        .child(Node.class, new NodeKey(nodeId)) //
        .build();
  }
}
