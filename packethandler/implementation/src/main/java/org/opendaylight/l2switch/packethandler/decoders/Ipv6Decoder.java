/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketOverRawReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketOverEthernetReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketOverEthernetReceivedBuilder;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPv6 Packet Decoder
 */
public class Ipv6Decoder extends AbstractPacketDecoder<EthernetPacketOverRawReceived, Ipv6PacketOverEthernetReceived>
  implements EthernetPacketListener {

  private static final Logger _logger = LoggerFactory.getLogger(Ipv6Decoder.class);

  public Ipv6Decoder(NotificationProviderService notificationProviderService) {
    super(Ipv6PacketOverEthernetReceived.class, notificationProviderService);
  }

  /**
   * Decode an EthernetPacket into an Ipv4Packet
   */
  @Override
  public Ipv6PacketOverEthernetReceived decode(EthernetPacketOverRawReceived ethernetPacketOverRawReceived) {

    Ipv6PacketOverEthernetReceivedBuilder builder = new Ipv6PacketOverEthernetReceivedBuilder();
    byte[] data = ethernetPacketOverRawReceived.getPayload();
    int offset = ethernetPacketOverRawReceived.getEthernetPacket().getPayloadOffset();
    //TODO: PLease note that the payload is original payload and to decode ethernet payload use payload offset from ethernet
    /*
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
    }
    catch (BufferException | UnknownHostException e) {
      _logger.debug("Exception while decoding IPv4 packet", e.getMessage());
    }
    */
    return builder.build();
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

    return KnownEtherType.Ipv6.equals(ethernetPacketOverRawReceived.getEthernetPacket().getEthertype());
  }
}
