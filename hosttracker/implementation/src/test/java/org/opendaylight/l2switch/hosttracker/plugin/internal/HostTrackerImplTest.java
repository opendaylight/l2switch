/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.l2switch.hosttracker.plugin.inventory.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.VlanId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

public class HostTrackerImplTest {

    @Mock
    private DataBroker dataBroker;
    private HostTrackerImpl hostTracker;

    public HostTrackerImplTest() {
        MockitoAnnotations.initMocks(this);
        hostTracker = PowerMockito.spy(new HostTrackerImpl(dataBroker));
    }

    /**
     * Test of registerAsDataChangeListener method, of class HostTrackerImpl.
     */
    @Test
    public void testRegisterAsDataChangeListener() {
        hostTracker.registerAsDataChangeListener();
        verify(dataBroker, times(3)).registerDataChangeListener(
                eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class),
                any(HostTrackerImpl.class),
                any(AsyncDataBroker.DataChangeScope.class));
    }

    /**
     * Test of onDataChanged method, of class HostTrackerImpl.
     */
    @Test
    public void testOnDataChanged_NullInput() {
        hostTracker.onDataChanged(null);
        verify(hostTracker, times(0)).packetReceived(any(Addresses.class), any(InstanceIdentifier.class));
        ConcurrentClusterAwareHostHashMap<HostId, Host> hosts = Whitebox.getInternalState(hostTracker, "hosts");
        assertEquals("Size of the \"hosts\" Set should be 0", 0, hosts.size());
    }

    /**
     * Test of onDataChanged method, of class HostTrackerImpl.
     */
    @Test
    public void testOnDataChanged_NullData() {

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> adce = Mockito.mock(AsyncDataChangeEvent.class);

        when(adce.getCreatedData()).thenReturn(null);
        when(adce.getUpdatedData()).thenReturn(null);
        when(adce.getRemovedPaths()).thenReturn(null);
        when(adce.getOriginalData()).thenReturn(null);
        hostTracker.onDataChanged(adce);
        verify(hostTracker, times(0)).packetReceived(any(Addresses.class), any(InstanceIdentifier.class));
        ConcurrentClusterAwareHostHashMap<HostId, Host> hosts = Whitebox.getInternalState(hostTracker, "hosts");
        assertEquals("Size of the \"hosts\" Set should be 0", 0, hosts.size());
    }

    /**
     * Test of onDataChanged method, of class HostTrackerImpl.
     */
    @Test
    public void testOnDataChanged_WithAddresses() throws InterruptedException, ExecutionException {

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> adce = Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<>();
        InstanceIdentifier<Addresses> iiAddrs = InstanceIdentifier.create(Addresses.class);
        Addresses addrs = new AddressesBuilder()
                .setId(new BigInteger("789"))
                .setFirstSeen(0L)
                .setLastSeen(0L)
                .setMac(new MacAddress("00:00:00:00:00:01"))
                .setIp(new IpAddress(new Ipv4Address("1.1.1.1")))
                .setVlan(new VlanId(0))
                .build();
        createdData.put(iiAddrs, addrs);
        System.out.println("iiAddrs " + iiAddrs);
        Map<InstanceIdentifier<?>, DataObject> emptyMap = new HashMap<>();
        HashSet<InstanceIdentifier<?>> emptySet = new HashSet<>();

        when(adce.getCreatedData()).thenReturn(createdData);
        when(adce.getUpdatedData()).thenReturn(emptyMap);
        when(adce.getRemovedPaths()).thenReturn(emptySet);
        when(adce.getOriginalData()).thenReturn(emptyMap);
        NodeConnector nc1 = new NodeConnectorBuilder()
                .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
                .build();
        NodeConnector nc2 = new NodeConnectorBuilder()
                .setKey(new NodeConnectorKey(new NodeConnectorId("2")))
                .build();
        NodeConnector nc3 = new NodeConnectorBuilder()
                .setKey(new NodeConnectorKey(new NodeConnectorId("3")))
                .build();

        List<NodeConnector> nodeConnectors = new ArrayList<>();
        nodeConnectors.add(nc1);
        nodeConnectors.add(nc2);
        nodeConnectors.add(nc3);
        Node node = new NodeBuilder()
                .setNodeConnector(nodeConnectors)
                .build();

        List< Node> nodeList;
        nodeList = new ArrayList<>();
        nodeList.add(node);
        Nodes nodes = new NodesBuilder().setNode(nodeList).build();
        Optional<Node> optionalNode = Optional.of(node);

        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFutNode = Mockito.mock(CheckedFuture.class);
        when(checkedFutNode.get()).thenReturn(optionalNode);
        CheckedFuture checkedFutNodeCon = Mockito.mock(CheckedFuture.class);
        when(checkedFutNodeCon.get()).thenReturn(nc1);

        InstanceIdentifier<NodeConnector> iinc = iiAddrs.firstIdentifierOf(NodeConnector.class);
        System.out.println("iinc " + iinc);
//        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> iin//
//                = iinc.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class);

        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), eq(iinc))).thenReturn(checkedFutNodeCon);
//        InstanceIdentifier<Node> iin = Mockito.mock(InstanceIdentifier.class, new MockSettingsImpl<InstanceIdentifier<NodeConnector>>());
//        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), iin)).thenReturn(checkedFutNode);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        hostTracker.onDataChanged(adce);
        verify(hostTracker, times(1)).packetReceived(any(Addresses.class), any(InstanceIdentifier.class));
        ConcurrentClusterAwareHostHashMap<HostId, Host> hosts = Whitebox.getInternalState(hostTracker, "hosts");
        assertEquals("Size of the \"hosts\" Set should be 1", 1, hosts.size());
    }

    /**
     * Test of packetReceived method, of class HostTrackerImpl.
     */
    @Test
    public void testPacketReceived() {

        InstanceIdentifier<Node> nInsId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).build();
//        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
//        InstanceIdentifier<Link> instanceId = InstanceIdentifier.create(Link.class);
//        Link nodeLink = new LinkBuilder().setLinkId(new LinkId("openflow:1")).build();
//        createdData.put(instanceId, nodeLink);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
//        System.out.println("packetReceived");
//        Addresses addrs = null;
//        InstanceIdentifier ii = null;
//        HostTrackerImpl instance = null;
//        instance.packetReceived(addrs, ii);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of close method, of class HostTrackerImpl.
     */
    @Test
    public void testClose() {
//        System.out.println("close");
//        HostTrackerImpl instance = null;
//        instance.close();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

//    private HostNode createHost(){
//        HostNodeBuilder hnb = new HostNodeBuilder();
//
//        hnb.setAddresses(null);
//    }
}
