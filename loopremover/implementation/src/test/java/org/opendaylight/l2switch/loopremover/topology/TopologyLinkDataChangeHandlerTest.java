/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.topology;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TopologyLinkDataChangeHandlerTest {

    @MockitoAnnotations.Mock
    private DataBroker dataBroker;
    @MockitoAnnotations.Mock
    private NetworkGraphService networkGraphService;
    private TopologyLinkDataChangeHandler topologyLinkDataChangeHandler;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataBroker, networkGraphService);
        topologyLinkDataChangeHandler.setTopologyId(null);
        topologyLinkDataChangeHandler.setGraphRefreshDelay(0);
    }

    @Test
    public void testRegisterAsDataChangeListener() throws Exception {
        topologyLinkDataChangeHandler.registerAsDataChangeListener();
        verify(dataBroker, times(1)).registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(DataTreeChangeListener.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_CreatedDataNoRefresh() throws Exception {
        InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
        Link hostLink = new LinkBuilder().setLinkId(new LinkId("host:1")).build();
        DataTreeModification<Link> mockChange = Mockito.mock(DataTreeModification.class);
        DataObjectModification<Link> mockModification = Mockito.mock(DataObjectModification.class);
        when(mockModification.getDataAfter()).thenReturn(hostLink);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(mockChange.getRootPath()).thenReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);
        topologyLinkDataChangeHandler.onDataTreeChanged(Collections.singletonList(mockChange));
        Thread.sleep(500);
        verify(networkGraphService, times(0)).clear();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_CreatedDataRefresh() throws Exception {
        InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
        Link hostLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
        DataTreeModification<Link> mockChange = Mockito.mock(DataTreeModification.class);
        DataObjectModification<Link> mockModification = Mockito.mock(DataObjectModification.class);
        when(mockModification.getDataAfter()).thenReturn(hostLink);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(mockChange.getRootPath()).thenReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);
        topologyLinkDataChangeHandler.onDataTreeChanged(Collections.singletonList(mockChange));
        Thread.sleep(500);
        verify(networkGraphService, times(1)).clear();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_RemovedDataNoRefresh() throws Exception {
        InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
        Link hostLink = new LinkBuilder().setLinkId(new LinkId("host:1")).build();
        DataTreeModification<Link> mockChange = Mockito.mock(DataTreeModification.class);
        DataObjectModification<Link> mockModification = Mockito.mock(DataObjectModification.class);
        when(mockModification.getDataBefore()).thenReturn(hostLink);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        when(mockChange.getRootPath()).thenReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);
        topologyLinkDataChangeHandler.onDataTreeChanged(Collections.singletonList(mockChange));
        Thread.sleep(500);
        verify(networkGraphService, times(0)).clear();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChanged_RemovedDataRefresh() throws Exception {
        InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
        Link hostLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
        DataTreeModification<Link> mockChange = Mockito.mock(DataTreeModification.class);
        DataObjectModification<Link> mockModification = Mockito.mock(DataObjectModification.class);
        when(mockModification.getDataBefore()).thenReturn(hostLink);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        when(mockChange.getRootPath()).thenReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);
        topologyLinkDataChangeHandler.onDataTreeChanged(Collections.singletonList(mockChange));
        Thread.sleep(500);
        verify(networkGraphService, times(1)).clear();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateNodeConnectorStatus_NoLinks() throws Exception {
        // Setup code to trigger the TopologyDataChangeEventProcessor
        InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
        Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
        DataTreeModification<Link> mockChange = Mockito.mock(DataTreeModification.class);
        DataObjectModification<Link> mockModification = Mockito.mock(DataObjectModification.class);
        when(mockModification.getDataAfter()).thenReturn(nodeLink);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(mockChange.getRootPath()).thenReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);
        // End setup code

        Topology topology = new TopologyBuilder().setLink(null).build();
        Optional<Topology> topologyOptional = Optional.of(topology);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(topologyOptional);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        topologyLinkDataChangeHandler.onDataTreeChanged(Collections.singletonList(mockChange));
        Thread.sleep(500);
        verify(dataBroker, times(0)).newReadWriteTransaction();
        verify(networkGraphService, times(0)).addLinks(any(List.class));
        verify(networkGraphService, times(0)).getAllLinks();
        verify(networkGraphService, times(0)).getLinksInMst();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateNodeConnectorStatus_WithLinks() throws Exception {
        // Setup code to trigger the TopologyDataChangeEventProcessor
        InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
        Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
        DataTreeModification<Link> mockChange = Mockito.mock(DataTreeModification.class);
        DataObjectModification<Link> mockModification = Mockito.mock(DataObjectModification.class);
        when(mockModification.getDataAfter()).thenReturn(nodeLink);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(mockChange.getRootPath()).thenReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);

        // getLinksFromTopology
        List<Link> links = new ArrayList<>();
        links.add(new LinkBuilder().setLinkId(new LinkId("openflow:1")).build());
        Topology topology = new TopologyBuilder().setLink(links).build();
        Optional<Topology> topologyOptional = Optional.of(topology);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(topologyOptional);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        // run
        ReadWriteTransaction readWriteTransaction = Mockito.mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);

        // updateNodeConnectorStatus
        Link link1 = new LinkBuilder().setLinkId(new LinkId("openflow:1"))
                .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1"))
                        .setSourceTp(new TpId("openflow:1")).build())
                .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:1"))
                        .setDestTp(new TpId("openflow:1")).build())
                .build();
        Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2"))
                .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2"))
                        .setSourceTp(new TpId("openflow:2")).build())
                .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2"))
                        .setDestTp(new TpId("openflow:2")).build())
                .build();

        List<Link> allLinks = new ArrayList<>();
        allLinks.add(link1);
        allLinks.add(link2);
        when(networkGraphService.getAllLinks()).thenReturn(allLinks);
        List<Link> mstLinks = new ArrayList<>();
        mstLinks.add(link1);
        when(networkGraphService.getLinksInMst()).thenReturn(mstLinks);

        // checkIfExistAndUpdateNodeConnector
        NodeConnector nodeConnector = new NodeConnectorBuilder().build();
        Optional<NodeConnector> optionalNodeConnector = Optional.of(nodeConnector);
        CheckedFuture checkedFutureNc = Mockito.mock(CheckedFuture.class);
        when(checkedFutureNc.get()).thenReturn(optionalNodeConnector);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(checkedFutureNc);

        topologyLinkDataChangeHandler.onDataTreeChanged(Collections.singletonList(mockChange));
        Thread.sleep(500);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        verify(networkGraphService, times(1)).addLinks(any(ArrayList.class));
        verify(networkGraphService, times(1)).getAllLinks();
        verify(networkGraphService, times(1)).getLinksInMst();
        verify(readWriteTransaction, times(4)).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(StpStatusAwareNodeConnector.class));
        verify(readWriteTransaction, times(1)).submit();
    }

    @Test
    public void testUpdateNodeConnectorStatus_WithStpLinks() throws Exception {
        // Setup code to trigger the TopologyDataChangeEventProcessor
        InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
        Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
        DataTreeModification<Link> mockChange = Mockito.mock(DataTreeModification.class);
        DataObjectModification<Link> mockModification = Mockito.mock(DataObjectModification.class);
        when(mockModification.getDataAfter()).thenReturn(nodeLink);
        when(mockModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(mockChange.getRootPath()).thenReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                instanceId));
        when(mockChange.getRootNode()).thenReturn(mockModification);

        // getLinksFromTopology
        List<Link> links = new ArrayList<>();
        links.add(new LinkBuilder().setLinkId(new LinkId("openflow:1")).build());
        Topology topology = new TopologyBuilder().setLink(links).build();
        Optional<Topology> topologyOptional = Optional.of(topology);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(topologyOptional);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        // run
        ReadWriteTransaction readWriteTransaction = Mockito.mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);

        // updateNodeConnectorStatus
        Link link1 = new LinkBuilder().setLinkId(new LinkId("openflow:1"))
                .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1"))
                        .setSourceTp(new TpId("openflow:1")).build())
                .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:1"))
                        .setDestTp(new TpId("openflow:1")).build())
                .build();
        Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2"))
                .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2"))
                        .setSourceTp(new TpId("openflow:2")).build())
                .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2"))
                        .setDestTp(new TpId("openflow:2")).build())
                .build();

        List<Link> allLinks = new ArrayList<>();
        allLinks.add(link1);
        allLinks.add(link2);
        when(networkGraphService.getAllLinks()).thenReturn(allLinks);
        List<Link> mstLinks = new ArrayList<>();
        mstLinks.add(link1);
        when(networkGraphService.getLinksInMst()).thenReturn(mstLinks);

        // checkIfExistAndUpdateNodeConnector
        StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder()
                .setStatus(StpStatus.Forwarding).build();
        NodeConnector nodeConnector = new NodeConnectorBuilder()
                .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector).build();
        Optional<NodeConnector> optionalNodeConnector = Optional.of(nodeConnector);
        CheckedFuture checkedFutureNc = Mockito.mock(CheckedFuture.class);
        when(checkedFutureNc.get()).thenReturn(optionalNodeConnector);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(checkedFutureNc);

        topologyLinkDataChangeHandler.onDataTreeChanged(Collections.singletonList(mockChange));
        Thread.sleep(500);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        verify(networkGraphService, times(1)).addLinks(any(ArrayList.class));
        verify(networkGraphService, times(1)).getAllLinks();
        verify(networkGraphService, times(1)).getLinksInMst();
        verify(readWriteTransaction, times(2)).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(StpStatusAwareNodeConnector.class));
        verify(readWriteTransaction, times(1)).submit();
    }

}
