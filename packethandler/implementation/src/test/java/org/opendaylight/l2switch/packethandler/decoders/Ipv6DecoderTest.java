/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6Packet;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

@ExtendWith(MockitoExtension.class)
class Ipv6DecoderTest {
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private NotificationService notificationService;

    private Ipv6Decoder ipv6Decoder;

    @BeforeEach
    void beforeEach() {
        ipv6Decoder = new Ipv6Decoder(notificationPublishService, notificationService);
    }

    @Test
    void testDecode() {
        byte[] payload = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            (byte)0x86, (byte)0xdd, // Ethernet EtherType
            0x60, // Version = 6,  DSCP = 3
            (byte)0xf0, 0x00, 0x01, // ECN = 3, FlowLabel = 1
            0x00, 0x05, // Length = 5
            0x11, // NextHeader = UDP
            (byte)0x0f, // HopLimit = 15
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Src IP Address (part1)
            (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Src IP Address (part2)
            (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Dest IP Address (part1)
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Dest IP Address (part2)
            0x01, 0x02, 0x03, 0x04, 0x05, // Data
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff // Ethernet Crc
        };

        var notification = ipv6Decoder.decode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setPayloadOffset(Uint32.valueOf(14)).build())
                    .build()))
            .setPayload(payload)
            .build());
        var ipv6Packet = assertInstanceOf(Ipv6Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(6), ipv6Packet.getVersion());
        assertEquals(new Dscp(Uint8.valueOf(3)), ipv6Packet.getDscp());
        assertEquals(Uint8.valueOf(3), ipv6Packet.getEcn());
        assertEquals(Uint32.ONE, ipv6Packet.getFlowLabel());
        assertEquals(Uint16.valueOf(5), ipv6Packet.getIpv6Length());
        assertEquals(KnownIpProtocols.Udp, ipv6Packet.getNextHeader());
        assertEquals(Uint8.valueOf(15), ipv6Packet.getHopLimit());
        assertEquals(new Ipv6Address("123:4567:89ab:cdef:fedc:ba98:7654:3210"), ipv6Packet.getSourceIpv6());
        assertEquals(new Ipv6Address("fedc:ba98:7654:3210:123:4567:89ab:cdef"), ipv6Packet.getDestinationIpv6());
        assertNull(ipv6Packet.getExtensionHeaders());
        assertEquals(Uint32.valueOf(54), ipv6Packet.getPayloadOffset());
        assertEquals(Uint32.valueOf(5), ipv6Packet.getPayloadLength());
        assertArrayEquals(payload, notification.getPayload());
    }

    @Test
    void testDecode_ExtensionHeader() {
        byte[] payload = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            (byte)0x86, (byte)0xdd, // Ethernet EtherType
            0x60, // Version = 6,  DSCP = 3
            (byte)0xf0, 0x00, 0x01, // ECN = 3, FlowLabel = 1
            0x00, 0x0d, // Length = 13
            0x00, // NextHeader = UDP
            (byte)0x0f, // HopLimit = 15
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Src IP Address (part1)
            (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Src IP Address (part2)
            (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Dest IP Address (part1)
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Dest IP Address (part2)
            0x11, 0x00, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, // Extension header
            0x01, 0x02, 0x03, 0x04, 0x05, // Data
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff // Ethernet Crc
        };

        var notification = ipv6Decoder.decode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setPayloadOffset(Uint32.valueOf(14)).build())
                    .build()))
            .setPayload(payload)
            .build());
        var ipv6Packet = assertInstanceOf(Ipv6Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(6), ipv6Packet.getVersion());
        assertEquals(new Dscp(Uint8.valueOf(3)), ipv6Packet.getDscp());
        assertEquals(Uint8.valueOf(3), ipv6Packet.getEcn());
        assertEquals(Uint32.ONE, ipv6Packet.getFlowLabel());
        assertEquals(Uint16.valueOf(13), ipv6Packet.getIpv6Length());
        assertEquals(KnownIpProtocols.Hopopt, ipv6Packet.getNextHeader());
        assertEquals(Uint8.valueOf(15), ipv6Packet.getHopLimit());
        assertEquals(new Ipv6Address("123:4567:89ab:cdef:fedc:ba98:7654:3210"), ipv6Packet.getSourceIpv6());
        assertEquals(new Ipv6Address("fedc:ba98:7654:3210:123:4567:89ab:cdef"), ipv6Packet.getDestinationIpv6());

        final var extensions = ipv6Packet.nonnullExtensionHeaders();
        assertEquals(1, extensions.size());

        final var extension = extensions.getFirst();
        assertEquals(KnownIpProtocols.Udp, extension.getNextHeader());
        assertEquals(Uint16.ZERO, extension.getLength());
        assertArrayEquals(Arrays.copyOfRange(payload, 56, 62), extension.getData());
        assertEquals(Uint32.valueOf(54), ipv6Packet.getPayloadOffset());
        assertEquals(Uint32.valueOf(13), ipv6Packet.getPayloadLength());
        assertArrayEquals(payload, notification.getPayload());
    }

    @Test
    void testDecode_ExtensionHeaders() {
        byte[] payload = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            (byte)0x86, (byte)0xdd, // Ethernet EtherType
            0x60, // Version = 6,  DSCP = 3
            (byte)0xf0, 0x00, 0x01, // ECN = 3, FlowLabel = 1
            0x00, 0x15, // Length = 21
            0x00, // NextHeader = UDP
            (byte)0x0f, // HopLimit = 15
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Src IP Address (part1)
            (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Src IP Address (part2)
            (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Dest IP Address (part1)
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Dest IP Address (part2)
            0x2b, 0x00, (byte)0xab, (byte)0xcd, (byte)0xef, (byte)0x12, (byte)0x34, (byte)0x56, // Extension header
            0x11, 0x00, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, // Extension header
            0x01, 0x02, 0x03, 0x04, 0x05, // Data
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff // Ethernet Crc
        };

        var notification = ipv6Decoder.decode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setPayloadOffset(Uint32.valueOf(14)).build())
                    .build()))
            .setPayload(payload)
            .build());
        var ipv6Packet = assertInstanceOf(Ipv6Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(6), ipv6Packet.getVersion());
        assertEquals(new Dscp(Uint8.valueOf(3)), ipv6Packet.getDscp());
        assertEquals(Uint8.valueOf(3), ipv6Packet.getEcn());
        assertEquals(Uint32.ONE, ipv6Packet.getFlowLabel());
        assertEquals(Uint16.valueOf(21), ipv6Packet.getIpv6Length());
        assertEquals(KnownIpProtocols.Hopopt, ipv6Packet.getNextHeader());
        assertEquals(Uint8.valueOf(15), ipv6Packet.getHopLimit());
        assertEquals(new Ipv6Address("123:4567:89ab:cdef:fedc:ba98:7654:3210"), ipv6Packet.getSourceIpv6());
        assertEquals(new Ipv6Address("fedc:ba98:7654:3210:123:4567:89ab:cdef"), ipv6Packet.getDestinationIpv6());

        final var extensions = ipv6Packet.nonnullExtensionHeaders();
        assertEquals(2, extensions.size());

        var extension = extensions.getFirst();
        assertEquals(KnownIpProtocols.Ipv6Route, extension.getNextHeader());
        assertEquals(Uint16.ZERO, extension.getLength());
        assertArrayEquals(Arrays.copyOfRange(payload, 56, 62), extension.getData());

        extension = extensions.get(1);
        assertEquals(KnownIpProtocols.Udp, extension.getNextHeader());
        assertEquals(Uint16.ZERO, extension.getLength());
        assertArrayEquals(Arrays.copyOfRange(payload, 64, 70), extension.getData());
        assertEquals(Uint32.valueOf(54), ipv6Packet.getPayloadOffset());
        assertEquals(Uint32.valueOf(21), ipv6Packet.getPayloadLength());
        assertArrayEquals(payload, notification.getPayload());
    }

    // This test is from a Mininet VM, taken from a wireshark dump
    @Test
    void testDecode_Udp() {
        byte[] payload = {
            // Ethernet start
            0x33, 0x33, 0x00, 0x00, 0x00, (byte)0xfb, (byte)0xa2, (byte)0xe6, (byte)0xda, 0x67, (byte)0xef, (byte)0x95,
            (byte)0x86, (byte)0xdd,
            // IPv6 packet start
            0x60, 0x00, 0x00, 0x00, 0x00, 0x35, 0x11, (byte)0xff, (byte)0xfe, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, (byte)0xa0, (byte)0xe6,
            (byte)0xda, (byte)0xff, (byte)0xfe, 0x67, (byte)0xef, (byte)0x95, (byte)0xff, 0x02, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xfb,
            // UDP start
            0x14, (byte)0xe9, 0x14, (byte)0xe9, 0x00, 0x35, 0x6b, (byte)0xd4, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x5f, 0x69, 0x70, 0x70, 0x73,
            0x04, 0x5f, 0x74, 0x63, 0x70, 0x05, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x00, 0x00, 0x0c, 0x00, 0x01,
            0x04, 0x5f, 0x69, 0x70, 0x70, (byte)0xc0, 0x12, 0x00, 0x0c, 0x00, 0x01
        };

        var notification = ipv6Decoder.decode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setPayloadOffset(Uint32.valueOf(14)).build())
                    .build()))
            .setPayload(payload)
            .build());
        var ipv6Packet = assertInstanceOf(Ipv6Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(6), ipv6Packet.getVersion());
        assertEquals(new Dscp(Uint8.ZERO), ipv6Packet.getDscp());
        assertEquals(Uint8.ZERO, ipv6Packet.getEcn());
        assertEquals(Uint32.ZERO, ipv6Packet.getFlowLabel());
        assertEquals(Uint16.valueOf(53), ipv6Packet.getIpv6Length());
        assertEquals(KnownIpProtocols.Udp, ipv6Packet.getNextHeader());
        assertEquals(Uint8.MAX_VALUE, ipv6Packet.getHopLimit());
        assertEquals(new Ipv6Address("fe80:0:0:0:a0e6:daff:fe67:ef95"), ipv6Packet.getSourceIpv6());
        assertEquals(new Ipv6Address("ff02:0:0:0:0:0:0:fb"), ipv6Packet.getDestinationIpv6());
        assertNull(ipv6Packet.getExtensionHeaders());
        assertEquals(Uint32.valueOf(54), ipv6Packet.getPayloadOffset());
        assertEquals(Uint32.valueOf(53), ipv6Packet.getPayloadLength());
        assertArrayEquals(payload, notification.getPayload());
    }

    @Test
    void testDecode_AlternatingBits() {
        byte[] payload = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            (byte)0x86, (byte)0xdd, // Ethernet EtherType
            0x60, // Version = 6,  DSCP = 0
            (byte)0x30, 0x00, 0x00, // ECN = 3, FlowLabel = 0
            0x00, 0x07, // Length = 7
            0x06, // NextHeader = TCP
            (byte)0x0f, // HopLimit = 15
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Src IP Address (part1)
            (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Src IP Address (part2)
            (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Dest IP Address (part1)
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Dest IP Address (part2)
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, // Data
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff // Ethernet Crc
        };

        var notification = ipv6Decoder.decode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder().setPayloadOffset(Uint32.valueOf(14)).build())
                    .build()))
            .setPayload(payload)
            .build());
        var ipv6Packet = assertInstanceOf(Ipv6Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(6), ipv6Packet.getVersion());
        assertEquals(new Dscp(Uint8.ZERO), ipv6Packet.getDscp());
        assertEquals(Uint8.valueOf(3), ipv6Packet.getEcn());
        assertEquals(Uint32.ZERO, ipv6Packet.getFlowLabel());
        assertEquals(Uint16.valueOf(7), ipv6Packet.getIpv6Length());
        assertEquals(KnownIpProtocols.Tcp, ipv6Packet.getNextHeader());
        assertEquals(Uint8.valueOf(15), ipv6Packet.getHopLimit());
        assertEquals(new Ipv6Address("123:4567:89ab:cdef:fedc:ba98:7654:3210"), ipv6Packet.getSourceIpv6());
        assertEquals(new Ipv6Address("fedc:ba98:7654:3210:123:4567:89ab:cdef"), ipv6Packet.getDestinationIpv6());
        assertNull(ipv6Packet.getExtensionHeaders());
        assertEquals(Uint32.valueOf(54), ipv6Packet.getPayloadOffset());
        assertEquals(Uint32.valueOf(7), ipv6Packet.getPayloadLength());
        assertArrayEquals(payload, notification.getPayload());
    }
}
