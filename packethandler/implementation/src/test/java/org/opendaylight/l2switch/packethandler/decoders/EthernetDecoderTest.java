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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.Header8021qType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.MatchBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

class EthernetDecoderTest {
    private final EthernetDecoder ethernetDecoder = new EthernetDecoder();

    @Test
    void testToString() {
        assertEquals("""
            EthernetDecoder{consumes=org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.\
            PacketReceived, produces=org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.\
            EthernetPacketReceived}""", ethernetDecoder.toString());
    }

    @Test
    void testDecode_IPv4EtherType() {
        byte[] packet = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            0x08, 0x00,
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
            (byte)0x98, (byte)0xfe, (byte)0xdc, (byte)0xba
        };
        var notification = ethernetDecoder.tryDecode(new PacketReceivedBuilder()
            .setPayload(packet)
            .setMatch(new MatchBuilder().build())
            .build());
        assertNotNull(notification);

        var ethernetPacket = assertInstanceOf(EthernetPacket.class,
            notification.nonnullPacketChain().get(1).getPacket());
        assertEquals(KnownEtherType.Ipv4, ethernetPacket.getEthertype());
        assertNull(ethernetPacket.getEthernetLength());
        assertNull(ethernetPacket.getHeader8021q());
        assertEquals(new MacAddress("01:23:45:67:89:ab"), ethernetPacket.getDestinationMac());
        assertEquals(new MacAddress("cd:ef:01:23:45:67"), ethernetPacket.getSourceMac());
        assertEquals(Uint32.valueOf(14), ethernetPacket.getPayloadOffset());
        assertEquals(Uint32.valueOf(14), ethernetPacket.getPayloadLength());
        assertEquals(Uint32.valueOf(2566839482L), ethernetPacket.getCrc());
        assertArrayEquals(packet, notification.getPayload());
    }

    @Test
    void testDecode_Length() {
        byte[] packet = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            0x00, 0x0e,
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22,
            0x00, (byte)0xfe, (byte)0xdc, (byte)0xba
        };
        var notification = ethernetDecoder.tryDecode(new PacketReceivedBuilder()
            .setPayload(packet)
            .setMatch(new MatchBuilder().build())
            .build());
        assertNotNull(notification);

        var ethernetPacket = assertInstanceOf(EthernetPacket.class,
            notification.nonnullPacketChain().get(1).getPacket());
        assertNull(ethernetPacket.getEthertype());
        assertEquals(Uint16.valueOf(14), ethernetPacket.getEthernetLength());
        assertNull(ethernetPacket.getHeader8021q());
        assertEquals(new MacAddress("01:23:45:67:89:ab"), ethernetPacket.getDestinationMac());
        assertEquals(new MacAddress("cd:ef:01:23:45:67"), ethernetPacket.getSourceMac());
        assertEquals(Uint32.valueOf(14), ethernetPacket.getPayloadOffset());
        assertEquals(Uint32.valueOf(13), ethernetPacket.getPayloadLength());
        assertEquals(Uint32.valueOf(16702650), ethernetPacket.getCrc());
        assertArrayEquals(packet, notification.getPayload());
    }

    @Test
    void testDecode_IPv6EtherTypeWith8021qHeader() {
        byte[] packet = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            (byte) 0x81, 0x00,
            (byte) 0xff, (byte) 0xff,
            (byte) 0x86, (byte) 0xdd,
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            0x00, (byte)0x00, (byte)0xdc, (byte)0xba
        };
        var notification = ethernetDecoder.tryDecode(new PacketReceivedBuilder()
            .setPayload(packet)
            .setMatch(new MatchBuilder().build())
            .build());
        assertNotNull(notification);

        var ethernetPacket = assertInstanceOf(EthernetPacket.class,
            notification.nonnullPacketChain().get(1).getPacket());
        assertEquals(KnownEtherType.Ipv6, ethernetPacket.getEthertype());
        assertNull(ethernetPacket.getEthernetLength());
        assertEquals(new MacAddress("01:23:45:67:89:ab"), ethernetPacket.getDestinationMac());
        assertEquals(new MacAddress("cd:ef:01:23:45:67"), ethernetPacket.getSourceMac());
        assertEquals(Uint32.valueOf(18), ethernetPacket.getPayloadOffset());
        assertEquals(Uint32.valueOf(8), ethernetPacket.getPayloadLength());
        assertEquals(Uint32.valueOf(56506), ethernetPacket.getCrc());
        assertArrayEquals(packet, notification.getPayload());

        final var dot1qs = ethernetPacket.nonnullHeader8021q();
        assertEquals(1, dot1qs.size());
        final var dot1q = dot1qs.getFirst();
        assertEquals(Header8021qType.VlanTagged, dot1q.getTPID());
        assertEquals(Uint8.valueOf(7), dot1q.getPriorityCode());
        assertTrue(dot1q.getDropEligible());
        assertEquals(new VlanId(Uint16.valueOf(4095)), dot1q.getVlan());
    }

    @Test
    void testDecode_IPv6EtherTypeWithQinQ() throws Exception {
        byte[] packet = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            (byte) 0x91, 0x00,
            (byte) 0xff, (byte) 0xff,
            (byte) 0x81, 0x00,
            (byte) 0xa0, (byte) 0x0a,
            (byte) 0x86, (byte) 0xdd,
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
            (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d
        };
        var notification = ethernetDecoder.tryDecode(new PacketReceivedBuilder()
            .setPayload(packet)
            .setMatch(new MatchBuilder().build())
            .build());
        assertNotNull(notification);

        var ethernetPacket = assertInstanceOf(EthernetPacket.class,
            notification.nonnullPacketChain().get(1).getPacket());
        assertEquals(KnownEtherType.Ipv6, ethernetPacket.getEthertype());
        assertNull(ethernetPacket.getEthernetLength());
        assertEquals(new MacAddress("01:23:45:67:89:ab"), ethernetPacket.getDestinationMac());
        assertEquals(new MacAddress("cd:ef:01:23:45:67"), ethernetPacket.getSourceMac());
        assertEquals(Uint32.valueOf(22), ethernetPacket.getPayloadOffset());
        assertEquals(Uint32.valueOf(14), ethernetPacket.getPayloadLength());
        assertEquals(Uint32.valueOf(168496141), ethernetPacket.getCrc());
        assertArrayEquals(packet, notification.getPayload());

        final var dot1qs = ethernetPacket.nonnullHeader8021q();
        assertEquals(2, dot1qs.size());

        var dot1q = dot1qs.getFirst();
        assertEquals(Header8021qType.QInQ, dot1q.getTPID());
        assertEquals(Uint8.valueOf(7), dot1q.getPriorityCode());
        assertTrue(dot1q.getDropEligible());
        assertEquals(new VlanId(Uint16.valueOf(4095)), dot1q.getVlan());

        dot1q = dot1qs.get(1);
        assertEquals(Header8021qType.VlanTagged, dot1q.getTPID());
        assertEquals(Uint8.valueOf(5), dot1q.getPriorityCode());
        assertFalse(dot1q.getDropEligible());
        assertEquals(new VlanId(Uint16.TEN), dot1q.getVlan());
    }

    // This test is from a Mininet VM, taken from a wireshark dump
    @Test
    void testDecode_Ipv6Udp() {
        byte[] packet = {
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
        var notification = ethernetDecoder.tryDecode(new PacketReceivedBuilder()
            .setPayload(packet)
            .setMatch(new MatchBuilder().build())
            .build());
        assertNotNull(notification);

        var ethernetPacket = assertInstanceOf(EthernetPacket.class,
            notification.nonnullPacketChain().get(1).getPacket());
        assertEquals(KnownEtherType.Ipv6, ethernetPacket.getEthertype());
        assertNull(ethernetPacket.getEthernetLength());
        assertNull(ethernetPacket.getHeader8021q());
        assertEquals(new MacAddress("33:33:00:00:00:fb"), ethernetPacket.getDestinationMac());
        assertEquals(new MacAddress("a2:e6:da:67:ef:95"), ethernetPacket.getSourceMac());
        assertEquals(Uint32.valueOf(14), ethernetPacket.getPayloadOffset());
        // Wirehshark didn't include a CRC, so not testing for length & crc fields
        assertArrayEquals(packet, notification.getPayload());
    }
}
