/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.Uint64;

@ExtendWith(MockitoExtension.class)
class AddressObservationWriterTest {
    private final MacAddress macAddress = new MacAddress("ba:43:52:ce:09:f4");
    private final IpAddress ipAddress = new IpAddress(new Ipv4Address("10.0.0.1"));
    private final NodeConnectorId ncId = new NodeConnectorId("foo");
    private final NodeConnectorRef realNcRef = new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
        .child(Node.class, new NodeKey(new NodeId("abc")))
        .child(NodeConnector.class, new NodeConnectorKey(ncId))
        .build());

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private AddressObservationWriter addressObservationWriter;

    @BeforeEach
    void beforeEach() {
        addressObservationWriter = new AddressObservationWriter(dataBroker);
        addressObservationWriter.setTimestampUpdateInterval(20L);
    }

    @Test
    void addAddressTest() throws Exception {
        doReturn(readTransaction).when(dataBroker).newReadOnlyTransaction();
        doReturn(FluentFutures.immediateFluentFuture(Optional.of(
            new NodeConnectorBuilder()
            .setId(ncId)
            .addAugmentation(new AddressCapableNodeConnectorBuilder()
                .setAddresses(BindingMap.of(new AddressesBuilder()
                    .setId(Uint64.ONE)
                    .setIp(ipAddress)
                    .setMac(macAddress)
                    .setLastSeen(1410350400L)
                    .setFirstSeen(1410350400L)
                    .build()))
                .build())
            .build()))).when(readTransaction).read(any(LogicalDatastoreType.class), any(DataObjectIdentifier.class));

        doReturn(writeTransaction).when(dataBroker).newWriteOnlyTransaction();
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();

        addressObservationWriter.addAddress(macAddress, ipAddress, realNcRef);
        verify(readTransaction, times(1)).read(any(LogicalDatastoreType.class), any(DataObjectIdentifier.class));
        verify(readTransaction, times(1)).close();
        verify(writeTransaction, times(1)).merge(any(LogicalDatastoreType.class), any(DataObjectIdentifier.class),
                any(AddressCapableNodeConnector.class));
        verify(writeTransaction, times(1)).commit();
    }

    @Test
    void addAddressNullTest() throws Exception {
        addressObservationWriter.addAddress(macAddress, null, realNcRef);
        verify(readTransaction, times(0)).read(any(LogicalDatastoreType.class), any(DataObjectIdentifier.class));
        verify(readTransaction, times(0)).close();
        verify(writeTransaction, times(0)).merge(any(LogicalDatastoreType.class), any(DataObjectIdentifier.class),
                any(AddressCapableNodeConnector.class));
        verify(writeTransaction, times(0)).commit();
    }
}
