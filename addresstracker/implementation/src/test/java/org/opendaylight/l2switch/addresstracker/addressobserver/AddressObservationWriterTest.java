/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AddressObservationWriterTest {

    private AddressCapableNodeConnector addrCapableNc;
    private ReadTransaction readTransaction;
    private WriteTransaction writeTransaction;
    private DataBroker dataService;
    private Optional<NodeConnector> dataObjectOptional = Optional.empty();
    private FluentFuture checkedFuture;
    private NodeConnector nodeConnector;
    private Addresses address;
    private AddressesKey addrKey;
    private MacAddress macAddress;
    private IpAddress ipAddress;
    private NodeConnectorRef realNcRef;

    @Before
    public void init() throws Exception {
        macAddress = new MacAddress("ba:43:52:ce:09:f4");
        ipAddress = new IpAddress(new Ipv4Address("10.0.0.1"));
        realNcRef = new NodeConnectorRef(
                InstanceIdentifier.builder(Nodes.class).child(Node.class).child(NodeConnector.class).build());

        readTransaction = mock(ReadTransaction.class);
        dataService = mock(DataBroker.class);
        when(dataService.newReadOnlyTransaction()).thenReturn(readTransaction);
        checkedFuture = mock(FluentFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(checkedFuture);
        //dataObjectOptional = mock(Optional.class);
        when(checkedFuture.get()).thenReturn(dataObjectOptional);
        nodeConnector = mock(NodeConnector.class);
        //when(dataObjectOptional.isPresent()).thenReturn(true);
        //when(dataObjectOptional.get()).thenReturn(nodeConnector);
        if (dataObjectOptional.isPresent()) {
            nodeConnector = dataObjectOptional.get();
        }

        addrCapableNc = mock(AddressCapableNodeConnector.class);
        when(nodeConnector.augmentation(AddressCapableNodeConnector.class)).thenReturn(addrCapableNc);

        address = mock(Addresses.class);
        List<Addresses> listAddr = new ArrayList<Addresses>();
        listAddr.add(address);
        //when(addrCapableNc.getAddresses().values()).thenReturn(listAddr);

        when(address.getIp()).thenReturn(ipAddress);
        when(address.getMac()).thenReturn(macAddress);
        when(address.getLastSeen()).thenReturn(1410350400L);
        when(address.getFirstSeen()).thenReturn(1410350400L);
        addrKey = mock(AddressesKey.class);
        when(address.key()).thenReturn(addrKey);

        writeTransaction = mock(WriteTransaction.class);
        when(dataService.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(writeTransaction.commit()).thenReturn(mock(FluentFuture.class));
    }

    @Test
    public void addAddressTest() throws Exception {
        AddressObservationWriter addressObservationWriter = new AddressObservationWriter(dataService);
        addressObservationWriter.setTimestampUpdateInterval(20L);
        addressObservationWriter.addAddress(macAddress, ipAddress, realNcRef);
        verify(readTransaction, times(1)).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(readTransaction, times(1)).close();
        verify(writeTransaction, times(0)).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(AddressCapableNodeConnector.class));
        verify(writeTransaction, times(0)).commit();
    }

    @Test
    public void addAddressNullTest() throws Exception {

        AddressObservationWriter addressObservationWriter = new AddressObservationWriter(dataService);
        addressObservationWriter.setTimestampUpdateInterval(20L);
        addressObservationWriter.addAddress(macAddress, null, realNcRef);
        verify(readTransaction, times(0)).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(readTransaction, times(0)).close();
        verify(writeTransaction, times(0)).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(AddressCapableNodeConnector.class));
        verify(writeTransaction, times(0)).commit();
    }
}
