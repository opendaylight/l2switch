package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.Header8021qType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.grp.Header8021q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.grp.Header8021qBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packethandler.packet.rev140528.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packethandler.packet.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packethandler.packet.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packethandler.packet.rev140528.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packethandler.packet.rev140528.packet.RawPacketBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Ethernet Packet Decoder
 */
public class EthernetDecoder implements PacketDecoder {
  private static final Logger _logger = LoggerFactory.getLogger(EthernetDecoder.class);
  public static final Integer LENGTH_MAX = 1500;
  public static final Integer ETHERTYPE_MIN = 1536;
  public static final Integer ETHERTYPE_8021Q = 0x8100;
  public static final Integer ETHERTYPE_QINQ = 0x9100;

  /**
   * Decode a RawPacket into an EthernetPacket
   *
   * @param packet -- data from wire to deserialize
   * @return
   * @throws BufferException
   */
  @Override
  public Packet decode(Packet packet) {
    RawPacket rawPacket = packet.getRawPacket();
    EthernetPacketBuilder builder = new EthernetPacketBuilder();
    byte[] data = rawPacket.getPayload();

    try {
      // Save original rawPacket
      builder.setRawPacket(new RawPacketBuilder().setIngress(rawPacket.getIngress()).setPayload(data).build());

      // Deserialize the destination & source fields
      builder.setDestinationMac(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 0, 48))));
      builder.setSourceMac(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 48, 48))));

      // Deserialize the optional field 802.1Q headers
      Integer nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 96, 16));
      int extraHeaderBits = 0;
      ArrayList<Header8021q> headerList = new ArrayList<Header8021q>();
      while(nextField.equals(ETHERTYPE_8021Q) || nextField.equals(ETHERTYPE_QINQ)) {
        Header8021qBuilder hBuilder = new Header8021qBuilder();
        hBuilder.setType(Header8021qType.forValue(nextField));

        // Read 2 more bytes for priority (3bits), drop eligible (1bit), vlan-id (12bits)
        byte[] vlanBytes = BitBufferHelper.getBits(data, 112 + extraHeaderBits, 16);

        // Remove the sign & right-shift to get the priority code
        hBuilder.setPriorityCode((short) ((vlanBytes[0] & 0xff) >> 5));

        // Remove the sign & remove priority code bits & right-shift to get drop-eligible bit
        hBuilder.setDropEligible(1 == (((vlanBytes[0] & 0xff) & 0x10) >> 4));

        // Remove priority code & drop-eligible bits, to get the VLAN-id
        vlanBytes[0] = (byte) (vlanBytes[0] & 0x0F);
        hBuilder.setVlan(BitBufferHelper.getInt(vlanBytes));

        // Add 802.1Q header to the growing collection
        headerList.add(hBuilder.build());

        // Reset value of "nextField" to correspond to following 2 bytes for next 802.1Q header or EtherType/Length
        nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 128 + extraHeaderBits, 16));

        // 802.1Q header means payload starts at a later position
        extraHeaderBits += 32;
      }
      // Set 802.1Q headers
      if(!headerList.isEmpty()) {
        builder.setHeader8021q(headerList);
      }

      // Deserialize the EtherType or Length field
      if(nextField >= ETHERTYPE_MIN) {
        builder.setEthertype(KnownEtherType.forValue(nextField));
      } else if(nextField <= LENGTH_MAX) {
        builder.setEthernetLength(nextField);
      } else {
        _logger.debug("Undefined header, value is not valid EtherType or length.  Value is " + nextField);
      }

      // Deserialize the payload now
      int payloadStart = 96 + 16 + extraHeaderBits;
      int payloadSize = data.length * NetUtils.NumBitsInAByte - payloadStart;
      int start = payloadStart / NetUtils.NumBitsInAByte;
      int stop = start + payloadSize / NetUtils.NumBitsInAByte;
      builder.setEthernetPayload(Arrays.copyOfRange(data, start, stop));

      if(null!=builder.getEthertype()) {
        builder.setPacketPayloadType(new PacketPayloadTypeBuilder()
            .setPacketType(PacketType.Ethernet)
            .setPayloadType(builder.getEthertype().getIntValue())
            .build());
      }

    } catch(BufferException be) {
      _logger.info("Exception during decoding raw packet to ethernet.");
    }

    return builder.build();

    //ToDo:  Possibly log these values
      /*if (_logger.isTraceEnabled()) {
        _logger.trace("{}: {}: {} (offset {} bitsize {})",
          new Object[] { this.getClass().getSimpleName(), hdrField,
            HexEncode.bytesToHexString(hdrFieldBytes),
            startOffset, numBits });
      }*/
  }

  @Override
  public Notification buildPacketNotification(Packet decodedPacket) {
    if(!(decodedPacket instanceof EthernetPacketGrp)) return null;

    EthernetPacketReceivedBuilder ethernetPacketReceivedBuilder = new EthernetPacketReceivedBuilder((EthernetPacketGrp) decodedPacket);
    return ethernetPacketReceivedBuilder.build();
  }
}
