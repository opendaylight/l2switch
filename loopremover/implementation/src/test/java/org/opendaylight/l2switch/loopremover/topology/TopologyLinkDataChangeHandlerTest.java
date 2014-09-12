/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.topology;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TopologyLinkDataChangeHandlerTest {

  @MockitoAnnotations.Mock private DataBroker dataBroker;
  @MockitoAnnotations.Mock private NetworkGraphService networkGraphService;
  private TopologyLinkDataChangeHandler topologyLinkDataChangeHandler;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataBroker, networkGraphService);
    topologyLinkDataChangeHandler.setGraphRefreshDelay(0);
  }

  @Test
  public void testRegisterAsDataChangeListener() throws Exception {
    topologyLinkDataChangeHandler.registerAsDataChangeListener();
    verify(dataBroker, times(1)).registerDataChangeListener(
      any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(TopologyLinkDataChangeHandler.class), any(AsyncDataBroker.DataChangeScope.class));
  }

  @Test
  public void testOnDataChanged_NullInput() throws Exception {

    topologyLinkDataChangeHandler.onDataChanged(null);
    Thread.sleep(500);
    verify(networkGraphService, times(0)).clear();
  }

  @Test
  public void testOnDataChanged_NullData() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    when(dataChangeEvent.getCreatedData()).thenReturn(null);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);
    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(500);
    verify(networkGraphService, times(0)).clear();
  }

  @Test
  public void testOnDataChanged_CreatedDataNoRefresh() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
    Link hostLink = new LinkBuilder().setLinkId(new LinkId("host:1")).build();
    createdData.put(instanceId, hostLink);
    when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);
    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(500);
    verify(networkGraphService, times(0)).clear();
  }


  @Test
  public void testOnDataChanged_CreatedDataRefresh() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
    Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
    createdData.put(instanceId, nodeLink);
    when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);
    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(500);
    verify(networkGraphService, times(1)).clear();
  }

  @Test
  public void testOnDataChanged_RemovedDataNoRefresh() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
    Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
    Link hostLink = new LinkBuilder().setLinkId(new LinkId("host:1")).build();
    originalData.put(instanceId, hostLink);
    removedPaths.add(instanceId);
    when(dataChangeEvent.getCreatedData()).thenReturn(null);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
    when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(500);
    verify(networkGraphService, times(0)).clear();
  }

  @Test
  public void testOnDataChanged_RemovedDataRefresh() throws Exception {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
    Map<InstanceIdentifier<?>, DataObject> originalData = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
    Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
    originalData.put(instanceId, nodeLink);
    removedPaths.add(instanceId);
    when(dataChangeEvent.getCreatedData()).thenReturn(null);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);
    when(dataChangeEvent.getOriginalData()).thenReturn(originalData);
    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(500);
    verify(networkGraphService, times(1)).clear();
  }

  @Test
  public void testUpdateNodeConnectorStatus_NoLinks() throws Exception {
    // Setup code to trigger the TopologyDataChangeEventProcessor
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
    Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
    createdData.put(instanceId, nodeLink);
    when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);
    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(500);
    // End setup code

    Topology topology = new TopologyBuilder().setLink(null).build();
    Optional<Topology> topologyOptional = Optional.of(topology);
    CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
    when(checkedFuture.get()).thenReturn(topologyOptional);
    ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
    when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
    when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

    verify(dataBroker, times(0)).newReadWriteTransaction();
    verify(networkGraphService, times(0)).addLinks(any(List.class));
    verify(networkGraphService, times(0)).getAllLinks();
    verify(networkGraphService, times(0)).getLinksInMst();
  }

  @Test
  public void testUpdateNodeConnectorStatus_WithLinks() throws Exception {
    // Setup code to trigger the TopologyDataChangeEventProcessor
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
    Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
    createdData.put(instanceId, nodeLink);
    when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);

    // getLinksFromTopology
    List<Link> links = new ArrayList<Link>();
    links.add(new LinkBuilder().setLinkId(new LinkId("openflow:1")).build());
    Topology topology = new TopologyBuilder().setLink(links).build();
    Optional<Topology> topologyOptional = Optional.of(topology);
    CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
    when(checkedFuture.get()).thenReturn(topologyOptional);
    ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
    when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
    when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

    // run
    ReadWriteTransaction readWriteTransaction = Mockito.mock(ReadWriteTransaction.class);
    when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);

    // updateNodeConnectorStatus
    Link link1 = new LinkBuilder().setLinkId(new LinkId("openflow:1"))
      .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
      .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:1")).setDestTp(new TpId("openflow:1")).build())
      .build();
    Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2"))
      .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
      .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2")).build())
      .build();

    List<Link> allLinks = new ArrayList<Link>();
    allLinks.add(link1);
    allLinks.add(link2);
    when(networkGraphService.getAllLinks()).thenReturn(allLinks);
    List<Link> mstLinks = new ArrayList<Link>();
    mstLinks.add(link1);
    when(networkGraphService.getLinksInMst()).thenReturn(mstLinks);

    // checkIfExistAndUpdateNodeConnector
    NodeConnector nodeConnector = new NodeConnectorBuilder().build();
    Optional<NodeConnector> optionalNodeConnector = Optional.of(nodeConnector);
    CheckedFuture checkedFutureNc = Mockito.mock(CheckedFuture.class);
    when(checkedFutureNc.get()).thenReturn(optionalNodeConnector);
    when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFutureNc);

    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(500);
    verify(dataBroker, times(1)).newReadWriteTransaction();
    verify(networkGraphService, times(1)).addLinks(any(ArrayList.class));
    verify(networkGraphService, times(1)).getAllLinks();
    verify(networkGraphService, times(1)).getLinksInMst();
    verify(readWriteTransaction, times(4)).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(StpStatusAwareNodeConnector.class));
    verify(readWriteTransaction, times(1)).submit();
  }

  @Test
  public void testUpdateNodeConnectorStatus_WithStpLinks() throws Exception {
    // Setup code to trigger the TopologyDataChangeEventProcessor
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
    Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
    Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
    createdData.put(instanceId, nodeLink);
    when(dataChangeEvent.getCreatedData()).thenReturn(createdData);
    when(dataChangeEvent.getRemovedPaths()).thenReturn(null);
    when(dataChangeEvent.getOriginalData()).thenReturn(null);

    // getLinksFromTopology
    List<Link> links = new ArrayList<Link>();
    links.add(new LinkBuilder().setLinkId(new LinkId("openflow:1")).build());
    Topology topology = new TopologyBuilder().setLink(links).build();
    Optional<Topology> topologyOptional = Optional.of(topology);
    CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
    when(checkedFuture.get()).thenReturn(topologyOptional);
    ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
    when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
    when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

    // run
    ReadWriteTransaction readWriteTransaction = Mockito.mock(ReadWriteTransaction.class);
    when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);

    // updateNodeConnectorStatus
    Link link1 = new LinkBuilder().setLinkId(new LinkId("openflow:1"))
      .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
      .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:1")).setDestTp(new TpId("openflow:1")).build())
      .build();
    Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2"))
      .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
      .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2")).build())
      .build();

    List<Link> allLinks = new ArrayList<Link>();
    allLinks.add(link1);
    allLinks.add(link2);
    when(networkGraphService.getAllLinks()).thenReturn(allLinks);
    List<Link> mstLinks = new ArrayList<Link>();
    mstLinks.add(link1);
    when(networkGraphService.getLinksInMst()).thenReturn(mstLinks);

    // checkIfExistAndUpdateNodeConnector
    StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder().setStatus(StpStatus.Forwarding).build();
    NodeConnector nodeConnector = new NodeConnectorBuilder()
      .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector)
      .build();
    Optional<NodeConnector> optionalNodeConnector = Optional.of(nodeConnector);
    CheckedFuture checkedFutureNc = Mockito.mock(CheckedFuture.class);
    when(checkedFutureNc.get()).thenReturn(optionalNodeConnector);
    when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFutureNc);

    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(500);
    verify(dataBroker, times(1)).newReadWriteTransaction();
    verify(networkGraphService, times(1)).addLinks(any(ArrayList.class));
    verify(networkGraphService, times(1)).getAllLinks();
    verify(networkGraphService, times(1)).getLinksInMst();
    verify(readWriteTransaction, times(2)).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(StpStatusAwareNodeConnector.class));
    verify(readWriteTransaction, times(1)).submit();
  }

}
