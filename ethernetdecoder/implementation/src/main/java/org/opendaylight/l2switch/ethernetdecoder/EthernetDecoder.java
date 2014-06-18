package org.opendaylight.l2switch.ethernetdecoder;

import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140616.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140616.ethernet.packet.grp.OuterTagBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140616.ethernet.packet.grp.outer.tag.InnerTagBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Ethernet Packet Decoder
 */
public class EthernetDecoder implements PacketDecoder {
  private static final Logger _logger = LoggerFactory.getLogger(EthernetDecoder.class);
  public static final Integer LENGTH_MAX = 1500;
  public static final Integer LENGTH_MIN = 64;
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

          //Check if we're dealing with 802.1Q or .1AD based on the first 2 bytes of the header

          if(nextField.equals(ETHERTYPE_QINQ) || nextField.equals(ETHERTYPE_8021Q)) {

              //TODO: Refactor the Outer & Inner tag building using a single method accepting a builder and nextfield.

              OuterTagBuilder _obuilder = new OuterTagBuilder();
              _obuilder.setTPID(Header8021qType.forValue(nextField));

              // Read 2 more bytes. For 802.1Q these are priority (3bits), drop eligible (1bit), vlan-id (12bits)
              byte[] vlanBytes = BitBufferHelper.getBits(data, 112 + extraHeaderBits, 16);

              // Remove the sign & right-shift to get the priority code
              _obuilder.setPriorityCode((short) ((vlanBytes[0] & 0xff) >> 5));

              // Remove the sign & remove priority code bits & right-shift to get drop-eligible bit
              _obuilder.setDropEligible(1 == (((vlanBytes[0] & 0xff) & 0x10) >> 4));

              // Remove priority code & drop-eligible bits, to get the VLAN-id
              vlanBytes[0] = (byte) (vlanBytes[0] & 0x0F);
              _obuilder.setVlan(new VlanId(BitBufferHelper.getInt(vlanBytes)));

              nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 128 + extraHeaderBits, 16));

              if(nextField.equals(ETHERTYPE_8021Q)) {

                  // 802.1ad header means payload starts at a later position
                  extraHeaderBits += 32;

                  InnerTagBuilder _ibuilder = new InnerTagBuilder();

                  _ibuilder.setTPID(Header8021qType.forValue(nextField));

                  // Read 2 more bytes. For 802.1Q these are priority (3bits), drop eligible (1bit), vlan-id (12bits)
                  vlanBytes = BitBufferHelper.getBits(data, 112 + extraHeaderBits, 16);

                  // Remove the sign & right-shift to get the priority code
                  _ibuilder.setPriorityCode((short) ((vlanBytes[0] & 0xff) >> 5));

                  // Remove the sign & remove priority code bits & right-shift to get drop-eligible bit
                  _ibuilder.setDropEligible(1 == (((vlanBytes[0] & 0xff) & 0x10) >> 4));

                  // Remove priority code & drop-eligible bits, to get the VLAN-id
                  vlanBytes[0] = (byte) (vlanBytes[0] & 0x0F);
                  _ibuilder.setVlan(new VlanId(BitBufferHelper.getInt(vlanBytes)));

                  _obuilder.setInnerTag(_ibuilder.build());

                  nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 128 + extraHeaderBits, 16));

              }

              builder.setOuterTag(_obuilder.build());

              // 802.1Q header means payload starts at a later position
              extraHeaderBits += 32;
          }

          // Deserialize the EtherType or Length field
          if(nextField >= ETHERTYPE_MIN) {
              builder.setEthertype(KnownEtherType.forValue(nextField));
          }
          //TODO: Handle padded frames ( for frames with 46<length<64)
          else if(nextField <= LENGTH_MAX && nextField >= LENGTH_MIN) {
              builder.setEthernetLength(nextField);
          } else {
              _logger.debug("Undefined header, value is not valid EtherType or invalid length.  Value is " + nextField);
          }

          // Deserialize the payload now
          int payloadStart = 96 + 16 + extraHeaderBits;
          int payloadSize = data.length * NetUtils.NumBitsInAByte - payloadStart;
          int start = payloadStart / NetUtils.NumBitsInAByte;
          int stop = start + payloadSize / NetUtils.NumBitsInAByte;
          builder.setEthernetPayload(Arrays.copyOfRange(data, start, stop));

          if(null != builder.getEthertype()) {
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
