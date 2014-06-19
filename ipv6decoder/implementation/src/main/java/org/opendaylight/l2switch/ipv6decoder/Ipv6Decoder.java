package org.opendaylight.l2switch.ipv6decoder;

import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.grp.ExtensionHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.grp.ExtensionHeadersBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IPv6 Packet Decoder
 */
public class Ipv6Decoder implements PacketDecoder {

  private static final Logger _logger = LoggerFactory.getLogger(Ipv6Decoder.class);

  /**
   * Decode an EthernetPacket into an Ipv4Packet
   */
  @Override
  public Packet decode(Packet packet) {
    if(!(packet instanceof EthernetPacketGrp)) return null;
    EthernetPacketGrp ethernetPacket = (EthernetPacketGrp) packet;

    Ipv6PacketBuilder builder = new Ipv6PacketBuilder();
    builder.fieldsFrom(ethernetPacket);
    byte[] data = ethernetPacket.getEthernetPayload();

    try {
      builder.setVersion(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 0, 4)));
      if (builder.getVersion().intValue() != 6) {
        _logger.debug("Version should be 6, but is " + builder.getVersion());
      }

      builder.setDscp(new Dscp(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 4, 6))));
      builder.setEcn(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 10, 2)));
      builder.setFlowLabel(BitBufferHelper.getLong(BitBufferHelper.getBits(data, 12, 20)));
      builder.setIpv6Length(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 32, 16)));
      builder.setNextHeader(KnownIpProtocols.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 48, 8))));
      builder.setHopLimit(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 56, 8)));
      builder.setSourceIpv6(InetAddress.getByAddress(BitBufferHelper.getBits(data, 64, 128)).getHostAddress());
      builder.setDestinationIpv6(InetAddress.getByAddress(BitBufferHelper.getBits(data, 192, 128)).getHostAddress());

      // Decode the optional "extension headers"
      List<ExtensionHeaders> extensionHeaders = new ArrayList<ExtensionHeaders>();
      int extraHeaderBits = 0;
      while (builder.getNextHeader() != null && !builder.getNextHeader().equals(KnownIpProtocols.Tcp) &&
        !builder.getNextHeader().equals(KnownIpProtocols.Udp)) {
        // Set the extension header's type & length & data
        int octetLength = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 328 + extraHeaderBits, 8));
        int start = ( 336 + extraHeaderBits) / NetUtils.NumBitsInAByte;
        int end = start + 6 + octetLength;

        extensionHeaders.add(new ExtensionHeadersBuilder()
          .setType(builder.getNextHeader())
          .setLength(octetLength)
          .setData(Arrays.copyOfRange(data, start, end))
          .build());

        // Update the NextHeader field
        builder.setNextHeader(KnownIpProtocols.forValue(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 320 + extraHeaderBits, 8))));
        extraHeaderBits += 64 + octetLength * NetUtils.NumBitsInAByte;
      }
      if (!extensionHeaders.isEmpty()) {
        builder.setExtensionHeaders(extensionHeaders);
      }

      // Decode the IPv6 Payload
      int start = ( 320 + extraHeaderBits) / NetUtils.NumBitsInAByte;
      int end = data.length;
      builder.setIpv6Payload(Arrays.copyOfRange(data, start, end));

      // Set packet payload type
      if (builder.getNextHeader() != null) {
        builder.setPacketPayloadType(new PacketPayloadTypeBuilder()
          .setPacketType(PacketType.Ipv4)  //ToDo: Fix this, add Ipv6
          .setPayloadType(builder.getNextHeader().getIntValue())
          .build());
      }
    }
    catch (BufferException | UnknownHostException e) {
      _logger.debug("Exception while decoding IPv4 packet", e.getMessage());
    }

    return builder.build();
  }

  @Override
  public Notification buildPacketNotification(Packet packet) {
    if(packet != null && packet instanceof Ipv6PacketGrp) {
      return new Ipv6PacketReceivedBuilder((Ipv6PacketGrp) packet).build();
    }
    return null;
  }

}
