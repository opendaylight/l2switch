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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

class Ipv4DecoderTest {
    private final Ipv4Decoder ipv4Decoder = new Ipv4Decoder();

    @Test
    void testToString() {
        assertEquals("""
            Ipv4Decoder{consumes=org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.\
            EthernetPacketReceived, produces=org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.\
            Ipv4PacketReceived}""", ipv4Decoder.toString());
    }

    @Test
    void testDecode() {
        byte[] ethPayload = {
            0x01, 0x23, 0x66, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            0x08, 0x00,
            0x45, // Version = 4,  IHL = 5
            0x00, // DSCP =0, ECN = 0
            0x00, 0x1E, // Total Length -- 30
            0x01, 0x1E, // Identification -- 286
            0x00, 0x00, // Flags = all off & Fragment offset = 0
            0x12, 0x11, // TTL = 18, Protocol = UDP
            0x00, 0x00, // Checksum = 0
            (byte) 0xc0, (byte) 0xa8, 0x00, 0x01, // Src IP Address
            0x01, 0x02, 0x03, 0x04, // Dest IP Address
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, // Data
            (byte) 0x98, (byte) 0xfe, (byte) 0xdc, (byte) 0xba // CRC
        };

        var notification = ipv4Decoder.tryDecode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder()
                        .setEthertype(KnownEtherType.Ipv4)
                        .setPayloadOffset(Uint32.valueOf(14))
                        .build())
                    .build()))
            .setPayload(ethPayload)
            .build());
        assertNotNull(notification);

        var ipv4Packet = assertInstanceOf(Ipv4Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(4), ipv4Packet.getVersion());
        assertEquals(Uint8.valueOf(5), ipv4Packet.getIhl());
        assertEquals(Uint16.valueOf(30), ipv4Packet.getIpv4Length());
        assertEquals(new Dscp(Uint8.ZERO), ipv4Packet.getDscp());
        assertEquals(Uint8.ZERO, ipv4Packet.getEcn());
        assertEquals(Uint16.valueOf(30), ipv4Packet.getIpv4Length());
        assertEquals(Uint16.valueOf(286), ipv4Packet.getId());
        assertFalse(ipv4Packet.getReservedFlag());
        assertFalse(ipv4Packet.getDfFlag());
        assertFalse(ipv4Packet.getMfFlag());
        assertEquals(Uint16.ZERO, ipv4Packet.getFragmentOffset());
        assertEquals(Uint8.valueOf(18), ipv4Packet.getTtl());
        assertEquals(KnownIpProtocols.Udp, ipv4Packet.getProtocol());
        assertEquals(Uint16.ZERO, ipv4Packet.getChecksum());

        assertEquals(new Ipv4Address("192.168.0.1"), ipv4Packet.getSourceIpv4());
        assertEquals(new Ipv4Address("1.2.3.4"), ipv4Packet.getDestinationIpv4());
        assertEquals(Uint32.TEN, ipv4Packet.getPayloadLength());
        assertEquals(Uint32.valueOf(34), ipv4Packet.getPayloadOffset());
        assertArrayEquals(ethPayload, notification.getPayload());
    }

    @Test
    void testDecode_WithDiffServAndFlagsAndOffset() {
        byte[] ethPayload = {
            0x01, 0x23, 0x66, 0x67, (byte) 0x89, (byte) 0xab, //src mac
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67, //dst mac
            (byte) 0x81, 0x00,
            0x08, 0x00, // EtherType
            0x45, // Version = 4,  IHL = 5
            (byte) 0xff, // DSCP =63, ECN = 3
            0x00, 0x1E, // Total Length -- 30
            0x01, 0x1E, // Identification -- 286
            (byte) 0xf0, 0x00, // Flags = all on & Fragment offset = 0
            0x12, 0x06, // TTL = 18, Protocol = TCP
            (byte) 0x00, 0x00, // Checksum = 0
            (byte) 0xc0, (byte) 0xa8, 0x00, 0x01, // Src IP Address
            0x01, 0x02, 0x03, 0x04, // Dest IP Address
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11,
            0x12, 0x13, // Data
            (byte) 0x98, (byte) 0xfe, (byte) 0xdc, (byte) 0xba // CRC
        };

        var notification = ipv4Decoder.tryDecode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder()
                        .setEthertype(KnownEtherType.Ipv4)
                        .setPayloadOffset(Uint32.valueOf(16))
                        .build())
                    .build()))
            .setPayload(ethPayload)
            .build());
        assertNotNull(notification);

        var ipv4Packet = assertInstanceOf(Ipv4Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(4), ipv4Packet.getVersion());
        assertEquals(Uint8.valueOf(5), ipv4Packet.getIhl());
        assertEquals(Uint16.valueOf(30), ipv4Packet.getIpv4Length());
        assertEquals(new Dscp(Uint8.valueOf(63)), ipv4Packet.getDscp());
        assertEquals(Uint8.valueOf(3), ipv4Packet.getEcn());
        assertEquals(Uint16.valueOf(30), ipv4Packet.getIpv4Length());
        assertEquals(Uint16.valueOf(286), ipv4Packet.getId());
        assertTrue(ipv4Packet.getReservedFlag());
        assertTrue(ipv4Packet.getDfFlag());
        assertTrue(ipv4Packet.getMfFlag());
        assertEquals(Uint16.valueOf(4096), ipv4Packet.getFragmentOffset());
        assertEquals(Uint8.valueOf(18), ipv4Packet.getTtl());
        assertEquals(KnownIpProtocols.Tcp, ipv4Packet.getProtocol());
        assertEquals(Uint16.ZERO, ipv4Packet.getChecksum());

        assertEquals(new Ipv4Address("192.168.0.1"), ipv4Packet.getSourceIpv4());
        assertEquals(new Ipv4Address("1.2.3.4"), ipv4Packet.getDestinationIpv4());
        assertEquals(Uint32.valueOf(13), ipv4Packet.getPayloadLength());
        assertEquals(Uint32.valueOf(36), ipv4Packet.getPayloadOffset());
        assertArrayEquals(ethPayload, notification.getPayload());
    }

    @Test
    void testDecode_AlternatingBits() {
        byte[] ethPayload = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            (byte) 0x81, 0x00,
            (byte) 0xff, (byte) 0xff,
            (byte) 0x86, (byte) 0xdd,
            (byte) 0xf5, // Version = 15,  IHL = 5
            (byte) 0x0f, // DSCP =3, ECN = 3
            0x00, 0x00, // Total Length -- 30
            (byte) 0xff, (byte) 0xff, // Identification -- 65535
            (byte) 0x1f, (byte) 0xff, // Flags = all off & Fragment offset = 8191
            0x00, 0x06, // TTL = 00, Protocol = TCP
            (byte) 0xff, (byte) 0xff, // Checksum = 65535
            (byte) 0x00, (byte) 0x00, 0x00, 0x00, // Src IP Address
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            // Dest IP Address
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, // Data
            (byte) 0x98, (byte) 0xfe, (byte) 0xdc, (byte) 0xba // CRC
        };

        var notification = ipv4Decoder.tryDecode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder()
                        .setEthertype(KnownEtherType.Ipv4)
                        .setPayloadOffset(Uint32.valueOf(18))
                        .build())
                    .build()))
            .setPayload(ethPayload)
            .build());
        assertNotNull(notification);

        var ipv4Packet = assertInstanceOf(Ipv4Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(15), ipv4Packet.getVersion());
        assertEquals(Uint8.valueOf(5), ipv4Packet.getIhl());
        assertEquals(Uint16.ZERO, ipv4Packet.getIpv4Length());
        assertEquals(new Dscp(Uint8.valueOf(3)), ipv4Packet.getDscp());
        assertEquals(Uint8.valueOf(3), ipv4Packet.getEcn());
        assertEquals(Uint16.ZERO, ipv4Packet.getIpv4Length());
        assertEquals(Uint16.MAX_VALUE, ipv4Packet.getId());
        assertFalse(ipv4Packet.getReservedFlag());
        assertFalse(ipv4Packet.getDfFlag());
        assertFalse(ipv4Packet.getMfFlag());
        assertEquals(Uint16.valueOf(8191), ipv4Packet.getFragmentOffset());
        assertEquals(Uint8.ZERO, ipv4Packet.getTtl());
        assertEquals(KnownIpProtocols.Tcp, ipv4Packet.getProtocol());
        assertEquals(Uint16.MAX_VALUE, ipv4Packet.getChecksum());

        assertEquals(new Ipv4Address("0.0.0.0"), ipv4Packet.getSourceIpv4());
        assertEquals(new Ipv4Address("255.255.255.255"), ipv4Packet.getDestinationIpv4());
        assertEquals(Uint32.TEN, ipv4Packet.getPayloadLength());
        assertEquals(Uint32.valueOf(38), ipv4Packet.getPayloadOffset());
        assertArrayEquals(ethPayload, notification.getPayload());
    }

    // This test is from a Mininet VM, taken from a wireshark dump
    @Test
    void testDecode_Udp() {
        byte[] ethPayload = {
            // Eth start
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x06, 0x27, 0x25, 0x06, (byte)0x81,
            (byte)0x81, 0x08, 0x00,
            // Ipv4 start
            0x45, 0x10, 0x01, 0x48, 0x00, 0x00, 0x00, 0x00, (byte)0x80, 0x11, 0x39, (byte)0x96, 0x00, 0x00, 0x00, 0x00,
            (byte)0xff, (byte)0xff,
            (byte)0xff, (byte)0xff,
            // Udp start
            0x00, 0x44, 0x00, 0x43, 0x01, 0x34, 0x2d, (byte)0xf5, 0x01, 0x01, 0x06, 0x00, (byte)0xdf, (byte)0xcc
        };

        var notification = ipv4Decoder.tryDecode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder()
                        .setEthertype(KnownEtherType.Ipv4)
                        .setPayloadOffset(Uint32.valueOf(14))
                        .build())
                    .build()))
            .setPayload(ethPayload)
            .build());
        assertNotNull(notification);

        var ipv4Packet = assertInstanceOf(Ipv4Packet.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(Uint8.valueOf(4), ipv4Packet.getVersion());
        assertEquals(Uint8.valueOf(5), ipv4Packet.getIhl());
        assertEquals(new Dscp(Uint8.valueOf(4)), ipv4Packet.getDscp());
        assertEquals(Uint8.valueOf(0), ipv4Packet.getEcn());
        assertEquals(Uint16.valueOf(328), ipv4Packet.getIpv4Length());
        assertEquals(Uint16.ZERO, ipv4Packet.getId());
        assertFalse(ipv4Packet.getReservedFlag());
        assertFalse(ipv4Packet.getDfFlag());
        assertFalse(ipv4Packet.getMfFlag());
        assertEquals(Uint16.ZERO, ipv4Packet.getFragmentOffset());
        assertEquals(Uint8.valueOf(128), ipv4Packet.getTtl());
        assertEquals(KnownIpProtocols.Udp, ipv4Packet.getProtocol());
        assertEquals(Uint16.valueOf(14742), ipv4Packet.getChecksum());

        assertEquals(new Ipv4Address("0.0.0.0"), ipv4Packet.getSourceIpv4());
        assertEquals(new Ipv4Address("255.255.255.255"), ipv4Packet.getDestinationIpv4());
        assertEquals(Uint32.valueOf(34), ipv4Packet.getPayloadOffset());
        // Not testing payloadLength because wireshark does not show crc
        assertArrayEquals(ethPayload, notification.getPayload());
    }
}
