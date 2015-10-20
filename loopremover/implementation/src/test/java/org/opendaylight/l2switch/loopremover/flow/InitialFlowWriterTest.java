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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InitialFlowWriterTest {

  @MockitoAnnotations.Mock private SalFlowService salFlowService;
  private InitialFlowWriter initialFlowWriter;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    initialFlowWriter = new InitialFlowWriter(salFlowService);
  }


  @Test
  public void onDataChange_Null() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    when(dataChangeEvent.getCreatedData()).thenReturn(null);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);
    initialFlowWriter.onDataChanged(dataChangeEvent);
    Thread.sleep(250);
    verify(salFlowService, times(0)).addFlow(any(AddFlowInput.class));
  }

  @Test
  public void onDataChange_Valid() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<Node> instanceId = InstanceIdentifier.builder(Nodes.class)
        .child(Node.class, new NodeKey(new NodeId("openflow:1")))
        .build();
    Node topoNode = new NodeBuilder().setId(new NodeId("openflow:1")).build();
    createdData.put(instanceId, topoNode);
    when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);
    initialFlowWriter.onDataChanged(dataChangeEvent);
    Thread.sleep(250);
    verify(salFlowService, times(1)).addFlow(any(AddFlowInput.class));

  }
}
