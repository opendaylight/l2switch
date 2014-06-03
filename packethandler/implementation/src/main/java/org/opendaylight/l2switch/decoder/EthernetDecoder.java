package decoder;

import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.types.rev140528.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.types.rev140528.ethernet.packet.Header8021qBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.types.rev140528.packet.types.EthernetPacketBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by alefan on 5/29/14.
 */
public class EthernetDecoder {
  private static final Logger _logger = LoggerFactory.getLogger(EthernetDecoder.class);
  public static final Integer LENGTH_MAX = 1500;
  public static final Integer ETHERTYPE_MIN = 1536;
  public static final Integer ETHERTYPE_8021Q = 0x8100;

  // payload, 0,payload.length * NetUtils.NumBitsInAByte

  /**
   * Decode a packet into an EthernetPacket
   * @param data - data from wire to deserialize
   * @param bitOffset - bit position where packet header starts in data
   * @return
   */
  public static EthernetPacket decode(byte[] data, int bitOffset) throws BufferException {
    EthernetPacketBuilder builder = new EthernetPacketBuilder();

    //0, 48, 96   = start pos
    //48, 48, 16  = length in bits

    //ToDo:  Possibly log these values
      /*if (_logger.isTraceEnabled()) {
        _logger.trace("{}: {}: {} (offset {} bitsize {})",
          new Object[] { this.getClass().getSimpleName(), hdrField,
            HexEncode.bytesToHexString(hdrFieldBytes),
            startOffset, numBits });
      }*/

    // Deserialize the destination & source fields
    builder.setDestination(BitBufferHelper.getBits(data, 0+bitOffset, 48));
    builder.setSource(BitBufferHelper.getBits(data, 48+bitOffset, 48));

    // Deserialize the optional field 802.1Q header
    Integer nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 96 + bitOffset, 16));
    int extraHeaderBits = 0;
    if (nextField.equals(ETHERTYPE_8021Q)) {
      // Read 2 more bytes for priority (3bits), drop eligible (1bit), vlan-id (12bits)
      byte[] vlanBytes = BitBufferHelper.getBits(data, 112+bitOffset, 16);

      Header8021qBuilder hBuilder = new Header8021qBuilder();
      // Remove the sign & right-shift to get the priority code
      hBuilder.setPriorityCode((short)((vlanBytes[0] & 0xff) >> 5));

      // Remove the sign & remove priority code bits & right-shift to get drop-eligible bit
      hBuilder.setDropEligible(1 == (((vlanBytes[0] & 0xff) & 0x10) >> 4));

      // Remove priority code & drop-eligible bits, to get the VLAN-id
      vlanBytes[0] = (byte)(vlanBytes[0] & 0x0F);
      hBuilder.setVlan(BitBufferHelper.getInt(vlanBytes));

      // Set 802.1Q header
      builder.setHeader8021q(Arrays.asList(hBuilder.build()));

      // Reset value of "nextField" to correspond to following 2 bytes for EtherType/Length
      nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 128+bitOffset, 16));

      // 802.1Q header means payload starts at a later position
      extraHeaderBits = 32;
    }

    // Deserialize the EtherType or Length field
    if (nextField >= ETHERTYPE_MIN) {
      builder.setEthertype(nextField);
    }
    else if (nextField <= LENGTH_MAX) {
      builder.setLength(nextField);
    }
    else {
      _logger.debug("Undefined header, value is not valid EtherType or length.  Value is " + nextField);
    }

    // Deserialize the payload now
    int payloadStart = 96 + 16 + extraHeaderBits + bitOffset;
    int payloadSize = data.length * NetUtils.NumBitsInAByte - payloadStart;
    int start = payloadStart / NetUtils.NumBitsInAByte;
    int stop = start + payloadSize / NetUtils.NumBitsInAByte;
    builder.setPayload(Arrays.copyOfRange(data, start, stop));

    return builder.build();
  }


  public static void main(String[] args) {

    // IPv4 EtherType (0x0800)
    // No 802.1Q header
    byte[] payload = {
      0x11, 0x22, 0x33, 0x44, 0x55, 0x66,
      0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
      0x08, 0x00,
      0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11
    };
    try {
      EthernetPacket e = decode(payload, 0);
      System.out.println(e);
    }
    catch (Exception e) {
    }

    // No EtherType, has the length field
    // No 802.1Q header
    byte[] payload2 = {
      0x11, 0x22, 0x33, 0x44, 0x55, 0x66,
      0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
      0x00, 0x0e,
      0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11
    };
    try {
      EthernetPacket e = decode(payload2, 0);
      System.out.println(e);
    }
    catch (Exception e) {
    }

    int i = 0x8100;
    short s = (short)0x8100;

    // Has EtherType
    // Has 802.1Q header
    byte[] payload3 = {
      0x11, 0x22, 0x33, 0x44, 0x55, 0x66,
      0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
      (byte)0x81, 0x00,
      (byte)0xff, (byte)0xff,
      0x08, 0x00,
      0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11
    };
    try {
      EthernetPacket e = decode(payload3, 0);
      System.out.println(e);
    }
    catch (Exception e) {
    }
  }
}
