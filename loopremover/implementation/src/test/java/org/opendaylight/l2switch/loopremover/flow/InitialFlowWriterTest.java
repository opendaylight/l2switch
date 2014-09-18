/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.flow;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

public class InitialFlowWriterTest {

  @MockitoAnnotations.Mock private SalFlowService salFlowService;
  private InitialFlowWriter initialFlowWriter;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    initialFlowWriter = new InitialFlowWriter(salFlowService);
  }


  @Test
  public void onNodeConnectorRemoved() throws Exception {
    NodeConnectorRemoved nodeConnectorRemoved = new NodeConnectorRemovedBuilder().build();
    initialFlowWriter.onNodeConnectorRemoved(nodeConnectorRemoved);
  }

  @Test
  public void onNodeConnectorUpdated() throws Exception {
    NodeConnectorUpdated nodeConnectorUpdated = new NodeConnectorUpdatedBuilder().build();
    initialFlowWriter.onNodeConnectorUpdated(nodeConnectorUpdated);
  }

  @Test
  public void onNodeRemoved() throws Exception {
    NodeRemoved nodeRemoved = new NodeRemovedBuilder().build();
    initialFlowWriter.onNodeRemoved(nodeRemoved);
  }

  @Test
  public void onNodeUpdated_Null() throws Exception {
    initialFlowWriter.onNodeUpdated(null);
    Thread.sleep(250);
    verify(salFlowService, times(0)).addFlow(any(AddFlowInput.class));
  }

  @Test
  public void onNodeUpdated_Valid() throws Exception {
    InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(""))).toInstance();
    NodeUpdated nodeUpdated = new NodeUpdatedBuilder().setNodeRef(new NodeRef(nodeInstanceIdentifier)).build();
    initialFlowWriter.onNodeUpdated(nodeUpdated);
    Thread.sleep(250);
    verify(salFlowService, times(1)).addFlow(any(AddFlowInput.class));
  }
}
