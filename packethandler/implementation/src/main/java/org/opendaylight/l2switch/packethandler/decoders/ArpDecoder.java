package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketOverEthernetReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketOverEthernetReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownHardwareType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.over.ethernet.fields.ArpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.over.ethernet.fields.EthernetOverRawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.over.ethernet.fields.EthernetOverRawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketOverRawReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ARP (Address Resolution Protocol) Packet Decoder
 */
public class ArpDecoder extends AbstractPacketDecoder<EthernetPacketOverRawReceived, ArpPacketOverEthernetReceived>
    implements EthernetPacketListener {

  private static final Logger _logger = LoggerFactory.getLogger(ArpDecoder.class);

  public ArpDecoder(NotificationProviderService notificationProviderService) {
    super(ArpPacketOverEthernetReceived.class, notificationProviderService);
  }

  /**
   * Decode an EthernetPacket into an ArpPacket
   */
  @Override
  public ArpPacketOverEthernetReceived decode(EthernetPacketOverRawReceived ethernetPacketOverRawReceived) {
    ArpPacketOverEthernetReceivedBuilder arpReceivedBuilder = new ArpPacketOverEthernetReceivedBuilder();

    byte[] data = ethernetPacketOverRawReceived.getPayload();
    int offset = ethernetPacketOverRawReceived.getEthernetPacket().getPayloadOffset();

    ArpPacketBuilder builder = new ArpPacketBuilder();

    try {
      // Decode the hardware-type (HTYPE) and protocol-type (PTYPE) fields
      builder.setHardwareType(KnownHardwareType.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, offset + 0, 16))));
      builder.setProtocolType(KnownEtherType.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, offset+16, 16))));

      // Decode the hardware-length and protocol-length fields
      builder.setHardwareLength(BitBufferHelper.getShort(BitBufferHelper.getBits(data, offset+32, 8)));
      builder.setProtocolLength(BitBufferHelper.getShort(BitBufferHelper.getBits(data, offset+40, 8)));

      // Decode the operation field
      builder.setOperation(KnownOperation.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, offset+48, 16))));

      // Decode the address fields
      int indexSrcProtAdd = 64 + 8 * builder.getHardwareLength();
      int indexDstHardAdd = indexSrcProtAdd + 8 * builder.getProtocolLength();
      int indexDstProtAdd = indexDstHardAdd + 8 * builder.getHardwareLength();
      if(builder.getHardwareType().equals(KnownHardwareType.Ethernet)) {
        builder.setSourceHardwareAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, offset + 64, 8 * builder.getHardwareLength())));
        builder.setDestinationHardwareAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, offset+indexDstHardAdd, 8 * builder.getHardwareLength())));
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

    //build arp
    arpReceivedBuilder.setArpPacket(builder.build());

    // extract ethernet over raw context from the consumed notification and set it and set it in Produced notification
    EthernetOverRawPacketBuilder ethernetOverRawPacketBuilder =  new EthernetOverRawPacketBuilder();
    ethernetOverRawPacketBuilder.setRawPacket(ethernetPacketOverRawReceived.getRawPacket());
    ethernetOverRawPacketBuilder.setEthernetPacket(ethernetPacketOverRawReceived.getEthernetPacket());

    //set the context of previous decoded fields
    arpReceivedBuilder.setEthernetOverRawPacket(ethernetOverRawPacketBuilder.build());

    // carry forward the original payload.
    arpReceivedBuilder.setPayload(ethernetPacketOverRawReceived.getPayload());

    return arpReceivedBuilder.build();
  }

  @Override
  public NotificationListener getConsumedNotificationListener() {
    return this;
  }

  @Override
  public void onEthernetPacketOverRawReceived(EthernetPacketOverRawReceived notification) {
    decodeAndPublish(notification);
  }

  @Override
  public boolean canDecode(EthernetPacketOverRawReceived ethernetPacketOverRawReceived) {
    if(ethernetPacketOverRawReceived==null || ethernetPacketOverRawReceived.getEthernetPacket()==null)
      return false;
    return KnownEtherType.Arp.equals(ethernetPacketOverRawReceived.getEthernetPacket().getEthertype());
  }
}
