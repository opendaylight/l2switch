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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4PacketBuilder;

import java.util.ArrayList;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AddressObserverUsingIpv4Test {

    private AddressObservationWriter addressObservationWriterMock;

    @Before
    public void init() {
        addressObservationWriterMock = mock(AddressObservationWriter.class);
    }

    @Test
    public void onIpv4PacketReceivedTest() throws Exception {
        ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new RawPacketBuilder().build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("")).build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new Ipv4PacketBuilder().setSourceIpv4(new Ipv4Address("1.2.3.4")).build())
            .build());

        Ipv4PacketReceived ipv4PktReceived = new Ipv4PacketReceivedBuilder().setPacketChain(packetChainList).build();
        AddressObserverUsingIpv4 addressObserverIpv4 = new AddressObserverUsingIpv4(addressObservationWriterMock);
        addressObserverIpv4.onIpv4PacketReceived(ipv4PktReceived);

        verify(addressObservationWriterMock, times(1)).addAddress(any(MacAddress.class), any(IpAddress.class),
            any(NodeConnectorRef.class));
    }

    @Test
    public void onIpv4PacketReceivedNullInputTest1() throws Exception {

        Ipv4PacketReceived ipv4PktReceived = new Ipv4PacketReceivedBuilder().setPacketChain(null).build();
        AddressObserverUsingIpv4 addressObserverIpv4 = new AddressObserverUsingIpv4(addressObservationWriterMock);
        addressObserverIpv4.onIpv4PacketReceived(ipv4PktReceived);

        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
            any(NodeConnectorRef.class));

    }

    @Test
    public void onIpv4PacketReceivedNullInputTest2() throws Exception {

        ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new RawPacketBuilder().build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("")).build())
            .build());

        Ipv4PacketReceived ipv4PktReceived = new Ipv4PacketReceivedBuilder().setPacketChain(packetChainList).build();
        AddressObserverUsingIpv4 addressObserverIpv4 = new AddressObserverUsingIpv4(addressObservationWriterMock);
        addressObserverIpv4.onIpv4PacketReceived(ipv4PktReceived);

        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
            any(NodeConnectorRef.class));

    }

    @Test
    public void onIpv4PacketReceivedNullInputTest3() throws Exception {

        ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new RawPacketBuilder().build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("")).build())
            .build());
        packetChainList.add(new PacketChainBuilder()
            .setPacket(new Ipv4PacketBuilder().setSourceIpv4(new Ipv4Address("0.0.0.0")).build())
            .build());

        Ipv4PacketReceived ipv4PktReceived = new Ipv4PacketReceivedBuilder().setPacketChain(packetChainList).build();
        AddressObserverUsingIpv4 addressObserverIpv4 = new AddressObserverUsingIpv4(addressObservationWriterMock);
        addressObserverIpv4.onIpv4PacketReceived(ipv4PktReceived);

        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
            any(NodeConnectorRef.class));

    }
}
