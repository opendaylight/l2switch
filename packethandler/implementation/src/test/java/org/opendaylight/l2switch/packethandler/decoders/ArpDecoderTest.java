/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownHardwareType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

@ExtendWith(MockitoExtension.class)
class ArpDecoderTest {
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private NotificationService notificationService;

    private ArpDecoder arpDecoder;

    @BeforeEach
    void beforeEach() {
        arpDecoder = new ArpDecoder(notificationPublishService, notificationService);
    }

    @Test
    void testDecode_RequestIPv4() {
        var notification = arpDecoder.decode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder()
                        .setPayloadOffset(Uint32.valueOf(5))
                        .setPayloadLength(Uint32.valueOf(33))
                        .build())
                    .build()))
            .setPayload(new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, // Offset is 5
                0x00, 0x01, // Hardware Type -- Ethernet
                0x08, 0x00, // Protocol Type -- Ipv4
                0x06, // Hardware Length -- 6
                0x04, // Protcool Length -- 4
                0x00, 0x01, // Operator -- Request
                0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, // Src Hardware Address
                (byte)0xc0, (byte)0xa8, 0x00, 0x01, // Src Protocol Address
                (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67, // Dest Hardware Address
                0x01, 0x02, 0x03, 0x04 // Dest Protocol Address
            })
            .build());

        var arpPacket = assertInstanceOf(ArpPacket.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(KnownHardwareType.Ethernet, arpPacket.getHardwareType());
        assertEquals(KnownEtherType.Ipv4, arpPacket.getProtocolType());
        assertEquals(Uint8.valueOf(6), arpPacket.getHardwareLength());
        assertEquals(Uint8.valueOf(4), arpPacket.getProtocolLength());
        assertEquals(KnownOperation.Request, arpPacket.getOperation());
        assertEquals("01:23:45:67:89:ab", arpPacket.getSourceHardwareAddress());
        assertEquals("192.168.0.1", arpPacket.getSourceProtocolAddress());
        assertEquals("cd:ef:01:23:45:67", arpPacket.getDestinationHardwareAddress());
        assertEquals("1.2.3.4", arpPacket.getDestinationProtocolAddress());
    }

    @Test
    void testDecode_ReplyIPv4() {
        var notification = arpDecoder.decode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder()
                        .setPayloadOffset(Uint32.valueOf(8))
                        .setPayloadLength(Uint32.valueOf(36))
                        .build())
                    .build()))
            .setPayload(new byte[] {
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Offset is 8
                0x00, 0x01, // Hardware Type -- Ethernet
                0x08, 0x00, // Protocol Type -- Ipv4
                0x06, // Hardware Length -- 6
                0x04, // Protcool Length -- 4
                0x00, 0x02, // Operator -- Reply
                0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, // Src Hardware Address
                (byte)0xc0, (byte)0xa8, 0x00, 0x01, // Src Protocol Address
                (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67, // Dest Hardware Address
                0x01, 0x02, 0x03, 0x04 // Dest Protocol Address
            })
            .build());

        var arpPacket = assertInstanceOf(ArpPacket.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(KnownHardwareType.Ethernet, arpPacket.getHardwareType());
        assertEquals(KnownEtherType.Ipv4, arpPacket.getProtocolType());
        assertEquals(Uint8.valueOf(6), arpPacket.getHardwareLength());
        assertEquals(Uint8.valueOf(4), arpPacket.getProtocolLength());
        assertEquals(KnownOperation.Reply, arpPacket.getOperation());
        assertEquals("01:23:45:67:89:ab", arpPacket.getSourceHardwareAddress());
        assertEquals("192.168.0.1", arpPacket.getSourceProtocolAddress());
        assertEquals("cd:ef:01:23:45:67", arpPacket.getDestinationHardwareAddress());
        assertEquals("1.2.3.4", arpPacket.getDestinationProtocolAddress());
    }

    // This test is from a Mininet VM, from a wireshark dump
    @Test
    void testDecode_Broadcast() {
        var notification = arpDecoder.decode(new EthernetPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder()
                    .setPacket(new EthernetPacketBuilder()
                    .setPayloadOffset(Uint32.valueOf(14))
                    .build())
                .build()))
            .setPayload(new byte[] {
                // Ethernet start
                (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xba, 0x43, 0x52,
                (byte)0xce, 0x09, (byte)0xf4, 0x08, 0x06,
                // Arp start
                0x00, 0x01, 0x08, 0x00, 0x06, 0x04, 0x00, 0x01, (byte)0xba, 0x43, 0x52, (byte)0xce, 0x09, (byte)0xf4,
                0x0a, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x02
            })
            .build());

        var arpPacket = assertInstanceOf(ArpPacket.class, notification.nonnullPacketChain().get(2).getPacket());
        assertEquals(KnownHardwareType.Ethernet, arpPacket.getHardwareType());
        assertEquals(KnownEtherType.Ipv4, arpPacket.getProtocolType());
        assertEquals(Uint8.valueOf(6), arpPacket.getHardwareLength());
        assertEquals(Uint8.valueOf(4), arpPacket.getProtocolLength());
        assertEquals(KnownOperation.Request, arpPacket.getOperation());
        assertEquals("ba:43:52:ce:09:f4", arpPacket.getSourceHardwareAddress());
        assertEquals("10.0.0.1", arpPacket.getSourceProtocolAddress());
        assertEquals("00:00:00:00:00:00", arpPacket.getDestinationHardwareAddress());
        assertEquals("10.0.0.2", arpPacket.getDestinationProtocolAddress());
    }
}
