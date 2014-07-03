/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

public class Ipv6DecoderTest {
  /*
    Ipv6Decoder ipv6Decoder = new Ipv6Decoder();

  @Test
  public void testDecode() throws Exception {
    byte[] payload = {
      0x60, // Version = 6,  DSCP = 3
      (byte)0xf0, 0x00, 0x01, // ECN = 3, FlowLabel = 1
      0x00, 0x05, // Length = 5
      0x11, // NextHeader = UDP
      (byte)0x0f, // HopLimit = 15
      0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Src IP Address (part1)
      (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Src IP Address (part2)
      (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Dest IP Address (part1)
      0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Dest IP Address (part2)
      0x01, 0x02, 0x03, 0x04, 0x05 // Data
    };
    Ipv6Packet ipv6packet = (Ipv6Packet)ipv6Decoder.decode(new EthernetPacketBuilder().setEthernetPayload(payload).build());
    assertEquals(6, ipv6packet.getVersion().intValue());
    assertEquals(3, ipv6packet.getDscp().getValue().intValue());
    assertEquals(3, ipv6packet.getEcn().intValue());
    assertEquals(1, ipv6packet.getFlowLabel().intValue());
    assertEquals(5, ipv6packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Udp, ipv6packet.getNextHeader());
    assertEquals(15, ipv6packet.getHopLimit().intValue());
    assertEquals("123:4567:89ab:cdef:fedc:ba98:7654:3210", ipv6packet.getSourceIpv6());
    assertEquals("fedc:ba98:7654:3210:123:4567:89ab:cdef", ipv6packet.getDestinationIpv6());
    assertNull(ipv6packet.getExtensionHeaders());
    assertTrue(Arrays.equals(ipv6packet.getIpv6Payload(), Arrays.copyOfRange(payload, 40, payload.length)));
  }

  @Test
  public void testDecode_ExtensionHeader() throws Exception {
    byte[] payload = {
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
      0x01, 0x02, 0x03, 0x04, 0x05 // Data
    };
    Ipv6Packet ipv6packet = (Ipv6Packet)ipv6Decoder.decode(new EthernetPacketBuilder().setEthernetPayload(payload).build());
    assertEquals(6, ipv6packet.getVersion().intValue());
    assertEquals(3, ipv6packet.getDscp().getValue().intValue());
    assertEquals(3, ipv6packet.getEcn().intValue());
    assertEquals(1, ipv6packet.getFlowLabel().intValue());
    assertEquals(13, ipv6packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Udp, ipv6packet.getNextHeader());
    assertEquals(15, ipv6packet.getHopLimit().intValue());
    assertEquals("123:4567:89ab:cdef:fedc:ba98:7654:3210", ipv6packet.getSourceIpv6());
    assertEquals("fedc:ba98:7654:3210:123:4567:89ab:cdef", ipv6packet.getDestinationIpv6());
    assertEquals(KnownIpProtocols.Hopopt, ipv6packet.getExtensionHeaders().get(0).getType());
    assertEquals(0, ipv6packet.getExtensionHeaders().get(0).getLength().intValue());
    assertTrue(Arrays.equals(ipv6packet.getExtensionHeaders().get(0).getData(), Arrays.copyOfRange(payload, 42, 48)));
    assertTrue(Arrays.equals(ipv6packet.getIpv6Payload(), Arrays.copyOfRange(payload, 48, payload.length)));
  }

  @Test
  public void testDecode_ExtensionHeaders() throws Exception {
    byte[] payload = {
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
      0x01, 0x02, 0x03, 0x04, 0x05 // Data
    };
    Ipv6Packet ipv6packet = (Ipv6Packet)ipv6Decoder.decode(new EthernetPacketBuilder().setEthernetPayload(payload).build());
    assertEquals(6, ipv6packet.getVersion().intValue());
    assertEquals(3, ipv6packet.getDscp().getValue().intValue());
    assertEquals(3, ipv6packet.getEcn().intValue());
    assertEquals(1, ipv6packet.getFlowLabel().intValue());
    assertEquals(21, ipv6packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Udp, ipv6packet.getNextHeader());
    assertEquals(15, ipv6packet.getHopLimit().intValue());
    assertEquals("123:4567:89ab:cdef:fedc:ba98:7654:3210", ipv6packet.getSourceIpv6());
    assertEquals("fedc:ba98:7654:3210:123:4567:89ab:cdef", ipv6packet.getDestinationIpv6());
    assertEquals(KnownIpProtocols.Hopopt, ipv6packet.getExtensionHeaders().get(0).getType());
    assertEquals(0, ipv6packet.getExtensionHeaders().get(0).getLength().intValue());
    assertTrue(Arrays.equals(ipv6packet.getExtensionHeaders().get(0).getData(), Arrays.copyOfRange(payload, 42, 48)));
    assertEquals(KnownIpProtocols.Ipv6Route, ipv6packet.getExtensionHeaders().get(1).getType());
    assertEquals(0, ipv6packet.getExtensionHeaders().get(1).getLength().intValue());
    assertTrue(Arrays.equals(ipv6packet.getExtensionHeaders().get(1).getData(), Arrays.copyOfRange(payload, 50, 56)));
    assertTrue(Arrays.equals(ipv6packet.getIpv6Payload(), Arrays.copyOfRange(payload, 56, payload.length)));
  }


  @Test
  public void testDecode_AlternatingBits() throws Exception {
    byte[] payload = {
      0x60, // Version = 6,  DSCP = 0
      (byte)0x30, 0x00, 0x00, // ECN = 3, FlowLabel = 0
      0x00, 0x07, // Length = 7
      0x06, // NextHeader = TCP
      (byte)0x0f, // HopLimit = 15
      0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Src IP Address (part1)
      (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Src IP Address (part2)
      (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, 0x76, 0x54, 0x32, 0x10, // Dest IP Address (part1)
      0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef, // Dest IP Address (part2)
      0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 // Data
    };
    Ipv6Packet ipv6packet = (Ipv6Packet)ipv6Decoder.decode(new EthernetPacketBuilder().setEthernetPayload(payload).build());
    assertEquals(6, ipv6packet.getVersion().intValue());
    assertEquals(0, ipv6packet.getDscp().getValue().intValue());
    assertEquals(3, ipv6packet.getEcn().intValue());
    assertEquals(0, ipv6packet.getFlowLabel().intValue());
    assertEquals(7, ipv6packet.getIpv6Length().intValue());
    assertEquals(KnownIpProtocols.Tcp, ipv6packet.getNextHeader());
    assertEquals(15, ipv6packet.getHopLimit().intValue());
    assertEquals("123:4567:89ab:cdef:fedc:ba98:7654:3210", ipv6packet.getSourceIpv6());
    assertEquals("fedc:ba98:7654:3210:123:4567:89ab:cdef", ipv6packet.getDestinationIpv6());
    assertNull(ipv6packet.getExtensionHeaders());
    assertTrue(Arrays.equals(ipv6packet.getIpv6Payload(), Arrays.copyOfRange(payload, 40, payload.length)));
  }
   */
}
