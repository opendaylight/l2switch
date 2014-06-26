package org.opendaylight.l2switch.packethandler.decoders;

public class Ipv4DecoderTest {
  /*Ipv4Decoder ipv4Decoder = new Ipv4Decoder();

  @Test
  public void testDecode() throws Exception {
    byte[] payload = {
      0x45, // Version = 4,  IHL = 5
      0x00, // DSCP =0, ECN = 0
      0x00, 0x1E, // Total Length -- 30
      0x01, 0x1E, // Identification -- 286
      0x00, 0x00, // Flags = all off & Fragment offset = 0
      0x12, 0x11, // TTL = 18, Protocol = UDP
      0x00, 0x00, // Checksum = 0
      (byte)0xc0, (byte)0xa8, 0x00, 0x01, // Src IP Address
      0x01, 0x02, 0x03, 0x04, // Dest IP Address
      0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10 // Data
    };
    Ipv4Packet ipv4packet = (Ipv4Packet)ipv4Decoder.decode(new EthernetPacketBuilder().setEthernetPayload(payload).build());
    assertEquals(4, ipv4packet.getVersion().intValue());
    assertEquals(5, ipv4packet.getIhl().intValue());
    assertEquals(30, ipv4packet.getIpv4Length().intValue());
    assertEquals(0, ipv4packet.getDscp().intValue());
    assertEquals(0, ipv4packet.getEcn().intValue());
    assertEquals(30, ipv4packet.getIpv4Length().intValue());
    assertEquals(286, ipv4packet.getId().intValue());
    assertFalse(ipv4packet.isReservedFlag());
    assertFalse(ipv4packet.isDfFlag());
    assertFalse(ipv4packet.isMfFlag());
    assertEquals(0, ipv4packet.getFragmentOffset().intValue());
    assertEquals(18, ipv4packet.getTtl().intValue());
    assertEquals(KnownIpProtocols.Udp, ipv4packet.getProtocol());
    assertEquals(0, ipv4packet.getChecksum().intValue());
    assertEquals("192.168.0.1", ipv4packet.getSourceIpv4());
    assertEquals("1.2.3.4", ipv4packet.getDestinationIpv4());
    assertTrue(Arrays.equals(ipv4packet.getIpv4Payload(), Arrays.copyOfRange(payload, 20, payload.length)));
  }

  @Test
  public void testDecode_WithDiffServAndFlagsAndOffset() throws Exception {
    byte[] payload = {
      0x45, // Version = 4,  IHL = 5
      (byte)0xff, // DSCP =63, ECN = 3
      0x00, 0x1E, // Total Length -- 30
      0x01, 0x1E, // Identification -- 286
      (byte)0xf0, 0x00, // Flags = all on & Fragment offset = 0
      0x12, 0x06, // TTL = 18, Protocol = TCP
      (byte)0x00, 0x00, // Checksum = 0
      (byte)0xc0, (byte)0xa8, 0x00, 0x01, // Src IP Address
      0x01, 0x02, 0x03, 0x04, // Dest IP Address
      0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10 // Data
    };
    Ipv4Packet ipv4packet = (Ipv4Packet)ipv4Decoder.decode(new EthernetPacketBuilder().setEthernetPayload(payload).build());
    assertEquals(4, ipv4packet.getVersion().intValue());
    assertEquals(5, ipv4packet.getIhl().intValue());
    assertEquals(30, ipv4packet.getIpv4Length().intValue());
    assertEquals(63, ipv4packet.getDscp().intValue());
    assertEquals(3, ipv4packet.getEcn().intValue());
    assertEquals(30, ipv4packet.getIpv4Length().intValue());
    assertEquals(286, ipv4packet.getId().intValue());
    assertTrue(ipv4packet.isReservedFlag());
    assertTrue(ipv4packet.isDfFlag());
    assertTrue(ipv4packet.isMfFlag());
    assertEquals(4096, ipv4packet.getFragmentOffset().intValue());
    assertEquals(18, ipv4packet.getTtl().intValue());
    assertEquals(KnownIpProtocols.Tcp, ipv4packet.getProtocol());
    assertEquals(0, ipv4packet.getChecksum().intValue());
    assertEquals("192.168.0.1", ipv4packet.getSourceIpv4());
    assertEquals("1.2.3.4", ipv4packet.getDestinationIpv4());
    assertTrue(Arrays.equals(ipv4packet.getIpv4Payload(), Arrays.copyOfRange(payload, 20, payload.length)));
  }

  @Test
  public void testDecode_AlternatingBits() throws Exception {
    byte[] payload = {
      (byte)0xf5, // Version = 15,  IHL = 5
      (byte)0x0f, // DSCP =3, ECN = 3
      0x00, 0x00, // Total Length -- 30
      (byte)0xff, (byte)0xff, // Identification -- 65535
      (byte)0x1f, (byte)0xff, // Flags = all off & Fragment offset = 8191
      0x00, 0x06, // TTL = 00, Protocol = TCP
      (byte)0xff, (byte)0xff, // Checksum = 65535
      (byte)0x00, (byte)0x00, 0x00, 0x00, // Src IP Address
      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, // Dest IP Address
      0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10 // Data
    };
    Ipv4Packet ipv4packet = (Ipv4Packet)ipv4Decoder.decode(new EthernetPacketBuilder().setEthernetPayload(payload).build());
    assertEquals(15, ipv4packet.getVersion().intValue());
    assertEquals(5, ipv4packet.getIhl().intValue());
    assertEquals(0, ipv4packet.getIpv4Length().intValue());
    assertEquals(3, ipv4packet.getDscp().intValue());
    assertEquals(3, ipv4packet.getEcn().intValue());
    assertEquals(0, ipv4packet.getIpv4Length().intValue());
    assertEquals(65535, ipv4packet.getId().intValue());
    assertFalse(ipv4packet.isReservedFlag());
    assertFalse(ipv4packet.isDfFlag());
    assertFalse(ipv4packet.isMfFlag());
    assertEquals(8191, ipv4packet.getFragmentOffset().intValue());
    assertEquals(0, ipv4packet.getTtl().intValue());
    assertEquals(KnownIpProtocols.Tcp, ipv4packet.getProtocol());
    assertEquals(65535, ipv4packet.getChecksum().intValue());
    assertEquals("0.0.0.0", ipv4packet.getSourceIpv4());
    assertEquals("255.255.255.255", ipv4packet.getDestinationIpv4());
    assertTrue(Arrays.equals(ipv4packet.getIpv4Payload(), Arrays.copyOfRange(payload, 20, payload.length)));
  }
  */
}
