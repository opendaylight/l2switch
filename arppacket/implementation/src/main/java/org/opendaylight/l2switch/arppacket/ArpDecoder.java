package org.opendaylight.l2switch.arppacket;

import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownHardwareType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ARP (Address Resolution Protocol) Packet Decoder
 */
public class ArpDecoder implements PacketDecoder {

  private static final Logger _logger = LoggerFactory.getLogger(ArpDecoder.class);

  /**
   * Decode an EthernetPacket into an ArpPacket
   */
  @Override
  public Packet decode(Packet packet) {
    ArpPacketBuilder builder = new ArpPacketBuilder();
    if(!(packet instanceof EthernetPacketGrp)) return null;

    EthernetPacketGrp ethernetPacket = (EthernetPacketGrp) packet;
    builder.fieldsFrom(ethernetPacket);
    byte[] data = ethernetPacket.getEthernetPayload();

    try {
      // Decode the hardware-type (HTYPE) and protocol-type (PTYPE) fields
      builder.setHardwareType(KnownHardwareType.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 0, 16))));
      builder.setProtocolType(KnownEtherType.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 16, 16))));

      // Decode the hardware-length and protocol-length fields
      builder.setHardwareLength(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 32, 8)));
      builder.setProtocolLength(BitBufferHelper.getShort(BitBufferHelper.getBits(data, 40, 8)));

      // Decode the operation field
      builder.setOperation(KnownOperation.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, 48, 16))));

      // Decode the address fields
      int indexSrcProtAdd = 64 + 8 * builder.getHardwareLength();
      int indexDstHardAdd = indexSrcProtAdd + 8 * builder.getProtocolLength();
      int indexDstProtAdd = indexDstHardAdd + 8 * builder.getHardwareLength();
      if(builder.getHardwareType().equals(KnownHardwareType.Ethernet)) {
        builder.setSourceHardwareAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 64, 8 * builder.getHardwareLength())));
        builder.setDestinationHardwareAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, indexDstHardAdd, 8 * builder.getHardwareLength())));
      } else {
        _logger.debug("Unknown HardwareType -- sourceHardwareAddress and destinationHardwareAddress are not decoded");
      }

      if(builder.getProtocolType().equals(KnownEtherType.Ipv4) || builder.getProtocolType().equals(KnownEtherType.Ipv6)) {
        builder.setSourceProtocolAddress(InetAddress.getByAddress(BitBufferHelper.getBits(data, indexSrcProtAdd, 8 * builder.getProtocolLength())).getHostAddress());
        builder.setDestinationProtocolAddress(InetAddress.getByAddress(BitBufferHelper.getBits(data, indexDstProtAdd, 8 * builder.getProtocolLength())).getHostAddress());
      } else {
        _logger.debug("Unknown ProtocolType -- sourceProtocolAddress and destinationProtocolAddress are not decoded");
      }
    } catch(BufferException | UnknownHostException e) {
      _logger.debug("Exception while decoding APR packet", e.getMessage());
    }

    return builder.build();
  }

  @Override
  public Notification buildPacketNotification(Packet arpPacket) {
    if(arpPacket == null) return null;

    if(arpPacket instanceof ArpPacketReceived) {
      return new ArpPacketReceivedBuilder((ArpPacketGrp) arpPacket).build();
    }

    return null;
  }

}
