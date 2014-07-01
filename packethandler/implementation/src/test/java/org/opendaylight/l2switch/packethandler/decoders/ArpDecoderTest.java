package org.opendaylight.l2switch.packethandler.decoders;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketOverEthernetReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownHardwareType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketOverRawReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.over.raw.fields.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.over.raw.fields.RawPacketBuilder;

import static junit.framework.Assert.*;

public class ArpDecoderTest {

  @Test
  public void testDecode_RequestIPv4() throws Exception {
    byte[] packet = {
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
    };
    NotificationProviderService mock = Mockito.mock(NotificationProviderService.class);
    ArpPacketOverEthernetReceived notification = new ArpDecoder(mock).decode(new EthernetPacketOverRawReceivedBuilder()
      .setEthernetPacket(new EthernetPacketBuilder().setPayloadOffset(5).setPayloadLength(33).build())
      .setPayload(packet)
      .build());

    assertEquals(KnownHardwareType.Ethernet, notification.getArpPacket().getHardwareType());
    assertEquals(KnownEtherType.Ipv4, notification.getArpPacket().getProtocolType());
    assertEquals(6, notification.getArpPacket().getHardwareLength().intValue());
    assertEquals(4, notification.getArpPacket().getProtocolLength().intValue());
    assertEquals(KnownOperation.Request, notification.getArpPacket().getOperation());
    assertEquals("01:23:45:67:89:ab", notification.getArpPacket().getSourceHardwareAddress());
    assertEquals("192.168.0.1", notification.getArpPacket().getSourceProtocolAddress());
    assertEquals("cd:ef:01:23:45:67", notification.getArpPacket().getDestinationHardwareAddress());
    assertEquals("1.2.3.4", notification.getArpPacket().getDestinationProtocolAddress());
  }

  /*
  @Test
  public void testDecode_ReplyIPv4() throws Exception {
    byte[] payload = {
      0x00, 0x01, // Hardware Type -- Ethernet
      0x08, 0x00, // Protocol Type -- Ipv4
      0x06, // Hardware Length -- 6
      0x04, // Protcool Length -- 4
      0x00, 0x02, // Operator -- Reply
      0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, // Src Hardware Address
      (byte)0xc0, (byte)0xa8, 0x00, 0x01, // Src Protocol Address
      (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67, // Dest Hardware Address
      0x01, 0x02, 0x03, 0x04 // Dest Protocol Address
    };
    ArpPacket arp = (ArpPacket)arpDecoder.decode(new EthernetPacketBuilder().setEthernetPayload(payload).build());
    assertEquals(KnownHardwareType.Ethernet, arp.getHardwareType());
    assertEquals(KnownEtherType.Ipv4, arp.getProtocolType());
    assertEquals(6, arp.getHardwareLength().intValue());
    assertEquals(4, arp.getProtocolLength().intValue());
    assertEquals(KnownOperation.Reply, arp.getOperation());
    assertEquals("01:23:45:67:89:ab", arp.getSourceHardwareAddress());
    assertEquals("192.168.0.1", arp.getSourceProtocolAddress());
    assertEquals("cd:ef:01:23:45:67", arp.getDestinationHardwareAddress());
    assertEquals("1.2.3.4", arp.getDestinationProtocolAddress());
  }
  */
}
