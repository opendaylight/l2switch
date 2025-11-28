/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

@ExtendWith(MockitoExtension.class)
class InventoryReaderTest {
    @Mock
    private DataBroker dataService;
    @Mock
    private ReadTransaction readOnlyTransaction;
    // @Mock
    // private Node node;

    private InventoryReader inventoryReader;
    private InstanceIdentifier<Node> nodeInstanceIdentifier;

    @BeforeEach
    void beforeEach() {
        inventoryReader = new InventoryReader(dataService);
    }

    @Test
    void getNodeConnectorTest() throws Exception {
        nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("node-id"))).build();
        when(dataService.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(DataObjectIdentifier.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()));

        long now = new Date().getTime();
        IpAddress ipAddress1 = new IpAddress(Ipv4Address.getDefaultInstance("10.0.0.1"));
        MacAddress macAddress1 = new MacAddress("00:00:00:00:00:01");
        final Addresses address1 = new AddressesBuilder()
            .setId(Uint64.ZERO)
            .setIp(ipAddress1)
            .setMac(macAddress1)
            .setFirstSeen(now)
            .setLastSeen(now)
            .build();
        IpAddress ipAddress2 = new IpAddress(Ipv4Address.getDefaultInstance("10.0.0.2"));
        MacAddress macAddress2 = new MacAddress("00:00:00:00:00:02");
        final Addresses address2 = new AddressesBuilder()
            .setId(Uint64.ONE)
            .setIp(ipAddress2)
            .setMac(macAddress2)
            .setFirstSeen(now)
            .setLastSeen(now)
            .build();
        AddressCapableNodeConnector addressCapableNodeConnector = new AddressCapableNodeConnectorBuilder()
                .setAddresses(BindingMap.of(address1, address2)).build();

        StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder()
                .setStatus(StpStatus.Discarding).build();

        NodeConnector nc1 = new NodeConnectorBuilder().withKey(new NodeConnectorKey(new NodeConnectorId("1"))).build();
        NodeConnector nc2 = new NodeConnectorBuilder().withKey(new NodeConnectorKey(new NodeConnectorId("2")))
                .addAugmentation(addressCapableNodeConnector).build();
        NodeConnector nc3 = new NodeConnectorBuilder().withKey(new NodeConnectorKey(new NodeConnectorId("3")))
                .addAugmentation(stpStatusAwareNodeConnector)
                .addAugmentation(addressCapableNodeConnector).build();
        NodeConnector ncLocal = new NodeConnectorBuilder().withKey(new NodeConnectorKey(new NodeConnectorId("LOCAL")))
                .addAugmentation(stpStatusAwareNodeConnector)
                .addAugmentation(addressCapableNodeConnector).build();

        List<NodeConnector> nodeConnectors = new ArrayList<>();
        nodeConnectors.add(nc1);
        nodeConnectors.add(nc2);
        nodeConnectors.add(nc3);
        nodeConnectors.add(ncLocal);

        // FIXME: re-activate this
        //when(node.getNodeConnector().values()).thenReturn(nodeConnectors);

        inventoryReader.getNodeConnector(nodeInstanceIdentifier, macAddress1);
    }
}
