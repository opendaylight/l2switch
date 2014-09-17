/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProactiveFloodFlowWriterTest {

  @MockitoAnnotations.Mock private DataBroker dataBroker;
  @MockitoAnnotations.Mock private SalFlowService salFlowService;
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

  @Test
  public void testRegisterAsDataChangeListener() throws Exception {
    proactiveFloodFlowWriter.registerAsDataChangeListener();
    verify(dataBroker, times(1)).registerDataChangeListener(
      any(LogicalDatastoreType.class),
      any(InstanceIdentifier.class),
      any(DataChangeListener.class),
      any(AsyncDataBroker.DataChangeScope.class));
  }

  @Test
  public void testOnDataChanged_NullInput() throws Exception {
    proactiveFloodFlowWriter.onDataChanged(null);
    verify(dataBroker, times(0)).newReadOnlyTransaction();
    verify(salFlowService, times(0)).addFlow(any(AddFlowInput.class));
  }

  @Test
  public void testOnDataChanged_NullData() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    when(dataChangeEvent.getCreatedData()).thenReturn(null);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);

    proactiveFloodFlowWriter.setFlowInstallationDelay(0);
    proactiveFloodFlowWriter.onDataChanged(dataChangeEvent);
    Thread.sleep(250);
    verify(dataBroker, times(0)).newReadOnlyTransaction();
    verify(salFlowService, times(0)).addFlow(any(AddFlowInput.class));
  }

  @Test
  public void testOnDataChanged_CreatedDataNoRefresh() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);

    proactiveFloodFlowWriter.setFlowInstallationDelay(0);
    proactiveFloodFlowWriter.onDataChanged(dataChangeEvent);
    Thread.sleep(250);
    verify(dataBroker, times(0)).newReadOnlyTransaction();
    verify(salFlowService, times(0)).addFlow(any(AddFlowInput.class));
  }

  @Test
  public void testOnDataChanged_RemovedDataNoRefresh() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
    Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();
    when(dataChangeEvent.getCreatedData()).thenReturn(null);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
    when(dataChangeEvent.getOriginalData()).thenReturn(originalData);

    proactiveFloodFlowWriter.setFlowInstallationDelay(0);
    proactiveFloodFlowWriter.onDataChanged(dataChangeEvent);
    Thread.sleep(250);
    verify(dataBroker, times(0)).newReadOnlyTransaction();
    verify(salFlowService, times(0)).addFlow(any(AddFlowInput.class));
  }


  @Test
  public void testOnDataChanged_CreatedDataRefresh() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    createdData.put(InstanceIdentifier.create(StpStatusAwareNodeConnector.class), null);
    when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);

    StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder().setStatus(StpStatus.Discarding).build();
    NodeConnector nc1 = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
      .build();
    NodeConnector nc2 = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("2")))
      .build();
    NodeConnector nc3 = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("3")))
      .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector)
      .build();
    NodeConnector ncLocal = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("LOCAL")))
      .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector)
      .build();

    List<NodeConnector> nodeConnectors = new ArrayList<NodeConnector>();
    nodeConnectors.add(nc1);
    nodeConnectors.add(nc2);
    nodeConnectors.add(nc3);
    nodeConnectors.add(ncLocal);
    Node node = new NodeBuilder()
      .setNodeConnector(nodeConnectors)
      .build();

    List<Node> nodeList = new ArrayList<Node>();
    nodeList.add(node);
    Nodes nodes = new NodesBuilder().setNode(nodeList).build();
    Optional<Nodes> optionalNodes = Optional.of(nodes);

    ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
    CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
    when(checkedFuture.get()).thenReturn(optionalNodes);
    when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
    when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

    proactiveFloodFlowWriter.setFlowInstallationDelay(0);
    proactiveFloodFlowWriter.onDataChanged(dataChangeEvent);
    Thread.sleep(250);
    verify(dataBroker, times(1)).newReadOnlyTransaction();
    verify(salFlowService, times(2)).addFlow(any(AddFlowInput.class));
  }
}
