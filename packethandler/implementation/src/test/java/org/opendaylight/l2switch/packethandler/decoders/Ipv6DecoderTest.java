/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6Packet;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;


public class Ipv6DecoderTest {

  @Test
  public void testDecode() throws Exception {
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
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(14).build())
      .build());

    Ipv6PacketReceived notification = new Ipv6Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(payload)
      .build());
    Ipv6Packet ipv6Packet = (Ipv6Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(6, ipv6Packet.getVersion().intValue());
    assertEquals(3, ipv6Packet.getDscp().getValue().intValue());
    assertEquals(3, ipv6Packet.getEcn().intValue());
    assertEquals(1, ipv6Packet.getFlowLabel().intValue());
    assertEquals(5, ipv6Packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Udp, ipv6Packet.getNextHeader());
    assertEquals(15, ipv6Packet.getHopLimit().intValue());
    assertEquals("123:4567:89ab:cdef:fedc:ba98:7654:3210", ipv6Packet.getSourceIpv6().getValue());
    assertEquals("fedc:ba98:7654:3210:123:4567:89ab:cdef", ipv6Packet.getDestinationIpv6().getValue());
    assertNull(ipv6Packet.getExtensionHeaders());
    assertEquals(54, ipv6Packet.getPayloadOffset().intValue());
    assertEquals(5, ipv6Packet.getPayloadLength().intValue());
    assertTrue(Arrays.equals(payload, notification.getPayload()));
  }

  @Test
  public void testDecode_ExtensionHeader() throws Exception {
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
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(14).build())
      .build());

    Ipv6PacketReceived notification = new Ipv6Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(payload)
      .build());
    Ipv6Packet ipv6Packet = (Ipv6Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(6, ipv6Packet.getVersion().intValue());
    assertEquals(3, ipv6Packet.getDscp().getValue().intValue());
    assertEquals(3, ipv6Packet.getEcn().intValue());
    assertEquals(1, ipv6Packet.getFlowLabel().intValue());
    assertEquals(13, ipv6Packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Hopopt, ipv6Packet.getNextHeader());
    assertEquals(15, ipv6Packet.getHopLimit().intValue());
    assertEquals("123:4567:89ab:cdef:fedc:ba98:7654:3210", ipv6Packet.getSourceIpv6().getValue());
    assertEquals("fedc:ba98:7654:3210:123:4567:89ab:cdef", ipv6Packet.getDestinationIpv6().getValue());
    assertEquals(KnownIpProtocols.Udp, ipv6Packet.getExtensionHeaders().get(0).getNextHeader());
    assertEquals(0, ipv6Packet.getExtensionHeaders().get(0).getLength().intValue());
    assertTrue(Arrays.equals(ipv6Packet.getExtensionHeaders().get(0).getData(), Arrays.copyOfRange(payload, 56, 62)));
    assertEquals(54, ipv6Packet.getPayloadOffset().intValue());
    assertEquals(13, ipv6Packet.getPayloadLength().intValue());
    assertTrue(Arrays.equals(payload, notification.getPayload()));
  }

  @Test
  public void testDecode_ExtensionHeaders() throws Exception {
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
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(14).build())
      .build());

    Ipv6PacketReceived notification = new Ipv6Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(payload)
      .build());
    Ipv6Packet ipv6Packet = (Ipv6Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(6, ipv6Packet.getVersion().intValue());
    assertEquals(3, ipv6Packet.getDscp().getValue().intValue());
    assertEquals(3, ipv6Packet.getEcn().intValue());
    assertEquals(1, ipv6Packet.getFlowLabel().intValue());
    assertEquals(21, ipv6Packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Hopopt, ipv6Packet.getNextHeader());
    assertEquals(15, ipv6Packet.getHopLimit().intValue());
    assertEquals("123:4567:89ab:cdef:fedc:ba98:7654:3210", ipv6Packet.getSourceIpv6().getValue());
    assertEquals("fedc:ba98:7654:3210:123:4567:89ab:cdef", ipv6Packet.getDestinationIpv6().getValue());
    assertEquals(KnownIpProtocols.Ipv6Route, ipv6Packet.getExtensionHeaders().get(0).getNextHeader());
    assertEquals(0, ipv6Packet.getExtensionHeaders().get(0).getLength().intValue());
    assertTrue(Arrays.equals(ipv6Packet.getExtensionHeaders().get(0).getData(), Arrays.copyOfRange(payload, 56, 62)));
    assertEquals(KnownIpProtocols.Udp, ipv6Packet.getExtensionHeaders().get(1).getNextHeader());
    assertEquals(0, ipv6Packet.getExtensionHeaders().get(1).getLength().intValue());
    assertTrue(Arrays.equals(ipv6Packet.getExtensionHeaders().get(1).getData(), Arrays.copyOfRange(payload, 64, 70)));
    assertEquals(54, ipv6Packet.getPayloadOffset().intValue());
    assertEquals(21, ipv6Packet.getPayloadLength().intValue());
    assertTrue(Arrays.equals(payload, notification.getPayload()));
  }

  // This test is from a Mininet VM, taken from a wireshark dump
  @Test
  public void testDecode_Udp() throws Exception {
    byte[] payload = {
      // Ethernet start
      0x33, 0x33, 0x00, 0x00, 0x00, (byte)0xfb, (byte)0xa2, (byte)0xe6, (byte)0xda, 0x67, (byte)0xef, (byte)0x95,
      (byte)0x86, (byte)0xdd,
      // IPv6 packet start
      0x60, 0x00, 0x00, 0x00, 0x00, 0x35, 0x11, (byte)0xff, (byte)0xfe, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xa0, (byte)0xe6,
      (byte)0xda, (byte)0xff, (byte)0xfe, 0x67, (byte)0xef, (byte)0x95, (byte)0xff, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xfb,
      // UDP start
      0x14, (byte)0xe9, 0x14, (byte)0xe9, 0x00, 0x35, 0x6b, (byte)0xd4, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x5f, 0x69, 0x70, 0x70, 0x73,
      0x04, 0x5f, 0x74, 0x63, 0x70, 0x05, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x00, 0x00, 0x0c, 0x00, 0x01,
      0x04, 0x5f, 0x69, 0x70, 0x70, (byte)0xc0, 0x12, 0x00, 0x0c, 0x00, 0x01
    };
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(14).build())
      .build());

    Ipv6PacketReceived notification = new Ipv6Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(payload)
      .build());
    Ipv6Packet ipv6Packet = (Ipv6Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(6, ipv6Packet.getVersion().intValue());
    assertEquals(0, ipv6Packet.getDscp().getValue().intValue());
    assertEquals(0, ipv6Packet.getEcn().intValue());
    assertEquals(0, ipv6Packet.getFlowLabel().intValue());
    assertEquals(53, ipv6Packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Udp, ipv6Packet.getNextHeader());
    assertEquals(255, ipv6Packet.getHopLimit().intValue());
    assertEquals("fe80:0:0:0:a0e6:daff:fe67:ef95", ipv6Packet.getSourceIpv6().getValue());
    assertEquals("ff02:0:0:0:0:0:0:fb", ipv6Packet.getDestinationIpv6().getValue());
    assertNull(ipv6Packet.getExtensionHeaders());
    assertEquals(54, ipv6Packet.getPayloadOffset().intValue());
    assertEquals(53, ipv6Packet.getPayloadLength().intValue());
    assertTrue(Arrays.equals(payload, notification.getPayload()));
  }

  @Test
  public void testDecode_AlternatingBits() throws Exception {
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
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(14).build())
      .build());

    Ipv6PacketReceived notification = new Ipv6Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(payload)
      .build());
    Ipv6Packet ipv6Packet = (Ipv6Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(6, ipv6Packet.getVersion().intValue());
    assertEquals(0, ipv6Packet.getDscp().getValue().intValue());
    assertEquals(3, ipv6Packet.getEcn().intValue());
    assertEquals(0, ipv6Packet.getFlowLabel().intValue());
    assertEquals(7, ipv6Packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Tcp, ipv6Packet.getNextHeader());
    assertEquals(15, ipv6Packet.getHopLimit().intValue());
    assertEquals("123:4567:89ab:cdef:fedc:ba98:7654:3210", ipv6Packet.getSourceIpv6().getValue());
    assertEquals("fedc:ba98:7654:3210:123:4567:89ab:cdef", ipv6Packet.getDestinationIpv6().getValue());
    assertNull(ipv6Packet.getExtensionHeaders());
    assertEquals(54, ipv6Packet.getPayloadOffset().intValue());
    assertEquals(7, ipv6Packet.getPayloadLength().intValue());
    assertTrue(Arrays.equals(payload, notification.getPayload()));
  }
}
