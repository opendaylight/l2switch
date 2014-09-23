/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6PacketBuilder;

import java.util.ArrayList;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AddressObserverUsingIpv6Test {

    private AddressObservationWriter addressObservationWriterMock;

    @Before
    public void init() {
        addressObservationWriterMock = mock(AddressObservationWriter.class);
    }

    @Test
    public void onIpv6PacketReceivedTest() {
        ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new RawPacketBuilder().build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("")).build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new Ipv6PacketBuilder().setSourceIpv6(new Ipv6Address("123:4567:89ab:cdef:fedc:ba98:7654:3210")).build())
            .build());

        Ipv6PacketReceived ipv6PktReceived = new Ipv6PacketReceivedBuilder().setPacketChain(packetChainList).build();
        AddressObserverUsingIpv6 addressObserverIpv6 = new AddressObserverUsingIpv6(addressObservationWriterMock);
        addressObserverIpv6.onIpv6PacketReceived(ipv6PktReceived);

        verify(addressObservationWriterMock, times(1)).addAddress(any(MacAddress.class), any(IpAddress.class),
            any(NodeConnectorRef.class));
    }

    @Test
    public void onIpv6PacketReceivedNullInputTest1() throws Exception {

        Ipv6PacketReceived ipv6PktReceived = new Ipv6PacketReceivedBuilder().setPacketChain(null).build();
        AddressObserverUsingIpv6 addressObserverIpv6 = new AddressObserverUsingIpv6(addressObservationWriterMock);
        addressObserverIpv6.onIpv6PacketReceived(ipv6PktReceived);

        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
            any(NodeConnectorRef.class));
    }

    @Test
    public void onIpv6PacketReceivedNullInputTest2() throws Exception {

        ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new RawPacketBuilder().build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("")).build())
            .build());

        Ipv6PacketReceived ipv6PktReceived = new Ipv6PacketReceivedBuilder().setPacketChain(packetChainList).build();
        AddressObserverUsingIpv6 addressObserverIpv6 = new AddressObserverUsingIpv6(addressObservationWriterMock);
        addressObserverIpv6.onIpv6PacketReceived(ipv6PktReceived);

        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
            any(NodeConnectorRef.class));
    }

    @Test
    public void onIpv6PacketReceivedNullInputTest3() throws Exception {

        ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new RawPacketBuilder().build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("")).build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new Ipv6PacketBuilder().setSourceIpv6(new Ipv6Address("0:0:0:0:0:0:0:0")).build())
            .build());

        Ipv6PacketReceived ipv6PktReceived = new Ipv6PacketReceivedBuilder().setPacketChain(packetChainList).build();
        AddressObserverUsingIpv6 addressObserverIpv6 = new AddressObserverUsingIpv6(addressObservationWriterMock);
        addressObserverIpv6.onIpv6PacketReceived(ipv6PktReceived);

        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
            any(NodeConnectorRef.class));
    }
}
