package org.opendaylight.l2switch.ipv4packet;

import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * IPv4 Packet Decoder
 */
public class Ipv4Decoder implements PacketDecoder {

  private static final Logger _logger = LoggerFactory.getLogger(Ipv4Decoder.class);

  /**
   * Decode an EthernetPacket into an Ipv4Packet
   */
  @Override
  public Packet decode(Packet packet) {
    if(!(packet instanceof EthernetPacketGrp)) return null;
    EthernetPacketGrp ethernetPacket = (EthernetPacketGrp) packet;

    Ipv4PacketBuilder builder = new Ipv4PacketBuilder();
    builder.fieldsFrom(ethernetPacket);
    byte[] data = ethernetPacket.getEthernetPayload();

    try {
      builder.setVersion(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 0, 4)));
      if (builder.getVersion().intValue() != 4) {
        _logger.debug("Version should be 4, but is " + builder.getVersion());
      }

      builder.setIhl(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 4, 4)));
      builder.setDscp(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 8, 6)));
      builder.setEcn(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 14, 2)));
      builder.setIpv4Length(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 16, 16)));
      builder.setId(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 32, 16)));

      // Decode the flags -- Reserved, DF (Don't Fragment), MF (More Fragments)
      builder.setReservedFlag(1 == (BitBufferHelper.getBits(data, 48, 1)[0] & 0xff));
      if (builder.isReservedFlag()) {
        _logger.debug("Reserved flag should be 0, but is 1.");
      }
      // "& 0xff" removes the sign of the Java byte
      builder.setDfFlag(1 == (BitBufferHelper.getBits(data, 49, 1)[0] & 0xff));
      builder.setMfFlag(1 == (BitBufferHelper.getBits(data, 50, 1)[0] & 0xff));

      builder.setFragmentOffset(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 51, 13)));
      builder.setTtl(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 64, 8)));
      builder.setProtocol(KnownIpProtocols.forValue(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 72, 8))));
      builder.setChecksum(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 80, 16)));
      builder.setSourceIpv4(InetAddress.getByAddress(BitBufferHelper.getBits(data, 96, 32)).getHostAddress());
      builder.setDestinationIpv4(InetAddress.getByAddress(BitBufferHelper.getBits(data, 128, 32)).getHostAddress());

      // Decode the optional "options" parameter
      int optionsSize = (builder.getIhl()-5)*32;
      if (optionsSize > 0) {
        builder.setIpv4Options(BitBufferHelper.getBits(data, 160, optionsSize));
      }

      // Decode the IPv4 Payload
      int payloadStartInBits = 160+optionsSize;
      int payloadEndInBits = data.length*NetUtils.NumBitsInAByte - payloadStartInBits;
      int start = payloadStartInBits/NetUtils.NumBitsInAByte;
      int end = start + payloadEndInBits/NetUtils.NumBitsInAByte;
      builder.setIpv4Payload(Arrays.copyOfRange(data, start, end));

      // Set packet payload type
      if (builder.getProtocol() != null) {
        builder.setPacketPayloadType(new PacketPayloadTypeBuilder()
          .setPacketType(PacketType.Ipv4)
          .setPayloadType(builder.getProtocol().getIntValue())
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
    if(packet != null && packet instanceof Ipv4PacketGrp) {
      return new Ipv4PacketReceivedBuilder((Ipv4PacketGrp) packet).build();
    }
    return null;
  }

}
