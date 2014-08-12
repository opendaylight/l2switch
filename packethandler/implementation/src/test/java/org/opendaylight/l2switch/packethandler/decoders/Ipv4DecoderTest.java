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

import java.util.ArrayList;
import java.util.Arrays;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Ipv4DecoderTest {

  @Test
  public void testDecode() throws Exception {
    byte[] eth_payload = {
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
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(14).build())
      .build());

    Ipv4PacketReceived notification = new Ipv4Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(eth_payload)
      .build());
    Ipv4Packet ipv4Packet = (Ipv4Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(4, ipv4Packet.getVersion().intValue());
    assertEquals(5, ipv4Packet.getIhl().intValue());
    assertEquals(30, ipv4Packet.getIpv4Length().intValue());
    assertEquals(0, ipv4Packet.getDscp().getValue().intValue());
    assertEquals(0, ipv4Packet.getEcn().intValue());
    assertEquals(30, ipv4Packet.getIpv4Length().intValue());
    assertEquals(286, ipv4Packet.getId().intValue());
    assertFalse(ipv4Packet.isReservedFlag());
    assertFalse(ipv4Packet.isDfFlag());
    assertFalse(ipv4Packet.isMfFlag());
    assertEquals(0, ipv4Packet.getFragmentOffset().intValue());
    assertEquals(18, ipv4Packet.getTtl().intValue());
    assertEquals(KnownIpProtocols.Udp, ipv4Packet.getProtocol());
    assertEquals(0, ipv4Packet.getChecksum().intValue());

    Ipv4Address src_address = new Ipv4Address("192.168.0.1");
    Ipv4Address dst_address = new Ipv4Address("1.2.3.4");
    assertEquals(src_address, ipv4Packet.getSourceIpv4());
    assertEquals(dst_address, ipv4Packet.getDestinationIpv4());
    assertEquals(10, ipv4Packet.getPayloadLength().intValue());
    assertEquals(34, ipv4Packet.getPayloadOffset().intValue());
    assertTrue(Arrays.equals(eth_payload, notification.getPayload()));
  }

  @Test
  public void testDecode_WithDiffServAndFlagsAndOffset() throws Exception {
    byte[] eth_payload = {
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
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(16).build())
      .build());

    Ipv4PacketReceived notification = new Ipv4Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(eth_payload)
      .build());
    Ipv4Packet ipv4Packet = (Ipv4Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(4, ipv4Packet.getVersion().intValue());
    assertEquals(5, ipv4Packet.getIhl().intValue());
    assertEquals(30, ipv4Packet.getIpv4Length().intValue());
    assertEquals(63, ipv4Packet.getDscp().getValue().intValue());
    assertEquals(3, ipv4Packet.getEcn().intValue());
    assertEquals(30, ipv4Packet.getIpv4Length().intValue());
    assertEquals(286, ipv4Packet.getId().intValue());
    assertTrue(ipv4Packet.isReservedFlag());
    assertTrue(ipv4Packet.isDfFlag());
    assertTrue(ipv4Packet.isMfFlag());
    assertEquals(4096, ipv4Packet.getFragmentOffset().intValue());
    assertEquals(18, ipv4Packet.getTtl().intValue());
    assertEquals(KnownIpProtocols.Tcp, ipv4Packet.getProtocol());
    assertEquals(0, ipv4Packet.getChecksum().intValue());

    Ipv4Address src_address = new Ipv4Address("192.168.0.1");
    Ipv4Address dst_address = new Ipv4Address("1.2.3.4");
    assertEquals(src_address, ipv4Packet.getSourceIpv4());
    assertEquals(dst_address, ipv4Packet.getDestinationIpv4());
    assertEquals(13, ipv4Packet.getPayloadLength().intValue());
    assertEquals(36, ipv4Packet.getPayloadOffset().intValue());
    assertTrue(Arrays.equals(eth_payload, notification.getPayload()));
  }

  @Test
  public void testDecode_AlternatingBits() throws Exception {
    byte[] eth_payload = {
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
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(18).build())
      .build());

    Ipv4PacketReceived notification = new Ipv4Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(eth_payload)
      .build());
    Ipv4Packet ipv4Packet = (Ipv4Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(15, ipv4Packet.getVersion().intValue());
    assertEquals(5, ipv4Packet.getIhl().intValue());
    assertEquals(0, ipv4Packet.getIpv4Length().intValue());
    assertEquals(3, ipv4Packet.getDscp().getValue().intValue());
    assertEquals(3, ipv4Packet.getEcn().intValue());
    assertEquals(0, ipv4Packet.getIpv4Length().intValue());
    assertEquals(65535, ipv4Packet.getId().intValue());
    assertFalse(ipv4Packet.isReservedFlag());
    assertFalse(ipv4Packet.isDfFlag());
    assertFalse(ipv4Packet.isMfFlag());
    assertEquals(8191, ipv4Packet.getFragmentOffset().intValue());
    assertEquals(0, ipv4Packet.getTtl().intValue());
    assertEquals(KnownIpProtocols.Tcp, ipv4Packet.getProtocol());
    assertEquals(65535, ipv4Packet.getChecksum().intValue());

    Ipv4Address src_address = new Ipv4Address("0.0.0.0");
    Ipv4Address dst_address = new Ipv4Address("255.255.255.255");
    assertEquals(src_address, ipv4Packet.getSourceIpv4());
    assertEquals(dst_address, ipv4Packet.getDestinationIpv4());
    assertEquals(10, ipv4Packet.getPayloadLength().intValue());
    assertEquals(38, ipv4Packet.getPayloadOffset().intValue());
    assertTrue(Arrays.equals(eth_payload, notification.getPayload()));
  }

  // This test is from a Mininet VM, taken from a wireshark dump
  @Test
  public void testDecode_Udp() throws Exception {
    byte[] eth_payload = {
      // Eth start
      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x06, 0x27, 0x25, 0x06, (byte)0x81, (byte)0x81, 0x08, 0x00,
      // Ipv4 start
      0x45, 0x10, 0x01, 0x48, 0x00, 0x00, 0x00, 0x00, (byte)0x80, 0x11, 0x39, (byte)0x96, 0x00, 0x00, 0x00, 0x00, (byte)0xff, (byte)0xff,
      (byte)0xff, (byte)0xff,
      // Udp start
      0x00, 0x44, 0x00, 0x43, 0x01, 0x34, 0x2d, (byte)0xf5, 0x01, 0x01, 0x06, 0x00, (byte)0xdf, (byte)0xcc
    };
    NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(14).build())
      .build());

    Ipv4PacketReceived notification = new Ipv4Decoder(npServiceMock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(eth_payload)
      .build());
    Ipv4Packet ipv4Packet = (Ipv4Packet)notification.getPacketChain().get(2).getPacket();
    assertEquals(4, ipv4Packet.getVersion().intValue());
    assertEquals(5, ipv4Packet.getIhl().intValue());
    assertEquals(4, ipv4Packet.getDscp().getValue().intValue());
    assertEquals(0, ipv4Packet.getEcn().intValue());
    assertEquals(328, ipv4Packet.getIpv4Length().intValue());
    assertEquals(0, ipv4Packet.getId().intValue());
    assertFalse(ipv4Packet.isReservedFlag());
    assertFalse(ipv4Packet.isDfFlag());
    assertFalse(ipv4Packet.isMfFlag());
    assertEquals(0, ipv4Packet.getFragmentOffset().intValue());
    assertEquals(128, ipv4Packet.getTtl().intValue());
    assertEquals(KnownIpProtocols.Udp, ipv4Packet.getProtocol());
    assertEquals(14742, ipv4Packet.getChecksum().intValue());

    Ipv4Address src_address = new Ipv4Address("0.0.0.0");
    Ipv4Address dst_address = new Ipv4Address("255.255.255.255");
    assertEquals(src_address, ipv4Packet.getSourceIpv4());
    assertEquals(dst_address, ipv4Packet.getDestinationIpv4());
    assertEquals(34, ipv4Packet.getPayloadOffset().intValue());
    // Not testing payloadLength because wireshark does not show crc
    assertTrue(Arrays.equals(eth_payload, notification.getPayload()));
  }
}
