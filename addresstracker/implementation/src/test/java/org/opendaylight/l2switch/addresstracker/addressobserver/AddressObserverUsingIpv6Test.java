/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.raw.packet.RawPacketFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6PacketBuilder;

@ExtendWith(MockitoExtension.class)
class AddressObserverUsingIpv6Test {
    @Mock
    private AddressObservationWriter addressObservationWriterMock;

    private AddressObserverUsingIpv6 addressObserverIpv6;

    @BeforeEach
    void beforeEach() {
        addressObserverIpv6 = new AddressObserverUsingIpv6(addressObservationWriterMock);
    }

    @Test
    void onIpv6PacketReceivedTest() {
        addressObserverIpv6.onNotification(new Ipv6PacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder()
                    .setPacket(new RawPacketBuilder().setRawPacketFields(new RawPacketFieldsBuilder().build()).build())
                    .build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("aa:bb:cc:dd:ee:ff")).build())
                    .build(),
                new PacketChainBuilder()
                    .setPacket(new Ipv6PacketBuilder()
                        .setSourceIpv6(new Ipv6Address("123:4567:89ab:cdef:fedc:ba98:7654:3210"))
                        .build())
                    .build()))
            .build());

        verify(addressObservationWriterMock, times(1)).addAddress(any(MacAddress.class), any(IpAddress.class), any());
    }

    @Test
    void onIpv6PacketReceivedNullInputTest1() throws Exception {
        addressObserverIpv6.onNotification(new Ipv6PacketReceivedBuilder().setPacketChain(null).build());
        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
                any(NodeConnectorRef.class));
    }

    @Test
    void onIpv6PacketReceivedNullInputTest2() throws Exception {
        addressObserverIpv6.onNotification(new Ipv6PacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("aa:bb:cc:dd:ee:ff")).build())
                    .build()))
            .build());
        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
                any(NodeConnectorRef.class));
    }

    @Test
    void onIpv6PacketReceivedNullInputTest3() throws Exception {
        addressObserverIpv6.onNotification(new Ipv6PacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setSourceMac(new MacAddress("aa:bb:cc:dd:ee:ff")).build())
                    .build(),
                new PacketChainBuilder()
                    .setPacket(new Ipv6PacketBuilder().setSourceIpv6(new Ipv6Address("0:0:0:0:0:0:0:0")).build())
                    .build()))
            .build());
        verify(addressObservationWriterMock, times(0)).addAddress(any(MacAddress.class), any(IpAddress.class),
                any(NodeConnectorRef.class));
    }
}
