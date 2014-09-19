/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class InventoryReaderTest {

    @MockitoAnnotations.Mock private DataBroker dataService;
    @MockitoAnnotations.Mock private ReadOnlyTransaction readOnlyTransaction;
    @MockitoAnnotations.Mock private Optional<Node> dataObjectOptional;
    @MockitoAnnotations.Mock private CheckedFuture checkedFuture;
    @MockitoAnnotations.Mock private Node node;

    private InventoryReader inventoryReader;
    private InstanceIdentifier<Node> nodeInstanceIdentifier;

    private NodeRef nodeRef;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        inventoryReader = new InventoryReader(dataService);
    }

    @Test
    public void getNodeConnectorTest() throws Exception{

        nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(
            Node.class, new NodeKey(new NodeId("node-id"))).toInstance();
        when(dataService.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class),
            any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(checkedFuture.get()) .thenReturn(dataObjectOptional);
        when(dataObjectOptional.isPresent()).thenReturn(true);
        when(dataObjectOptional.get()).thenReturn(node);

        long now = new Date().getTime();
        IpAddress ipAddress1 = new IpAddress(Ipv4Address.getDefaultInstance("10.0.0.1"));
        MacAddress macAddress1 = new MacAddress("00:00:00:00:00:01");
        final Addresses address1 = new AddressesBuilder()
            .setIp(ipAddress1)
            .setMac(macAddress1)
            .setFirstSeen(now)
            .setLastSeen(now)
            .build();
        IpAddress ipAddress2 = new IpAddress(Ipv4Address.getDefaultInstance("10.0.0.2"));
        MacAddress macAddress2 = new MacAddress("00:00:00:00:00:02");
        final Addresses address2 = new AddressesBuilder()
            .setIp(ipAddress2)
            .setMac(macAddress2)
            .setFirstSeen(now)
            .setLastSeen(now)
            .build();
        List<Addresses> addressList = new ArrayList<Addresses>();
        addressList.add(address1);
        addressList.add(address2);
        AddressCapableNodeConnector addressCapableNodeConnector = new AddressCapableNodeConnectorBuilder()
            .setAddresses(addressList).build();

        StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder()
            .setStatus(StpStatus.Discarding).build();

        NodeConnector nc1 = new NodeConnectorBuilder()
            .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
            .build();
        NodeConnector nc2 = new NodeConnectorBuilder()
            .setKey(new NodeConnectorKey(new NodeConnectorId("2")))
            .addAugmentation(AddressCapableNodeConnector.class, addressCapableNodeConnector)
            .build();
        NodeConnector nc3 = new NodeConnectorBuilder()
            .setKey(new NodeConnectorKey(new NodeConnectorId("3")))
            .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector)
            .addAugmentation(AddressCapableNodeConnector.class, addressCapableNodeConnector)
            .build();
        NodeConnector ncLocal = new NodeConnectorBuilder()
            .setKey(new NodeConnectorKey(new NodeConnectorId("LOCAL")))
            .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector)
            .addAugmentation(AddressCapableNodeConnector.class, addressCapableNodeConnector)
            .build();

        List<NodeConnector> nodeConnectors = new ArrayList<NodeConnector>();
        nodeConnectors.add(nc1);
        nodeConnectors.add(nc2);
        nodeConnectors.add(nc3);
        nodeConnectors.add(ncLocal);

        when(node.getNodeConnector()).thenReturn(nodeConnectors);

        inventoryReader.getNodeConnector(nodeInstanceIdentifier, macAddress1);

    }
}
