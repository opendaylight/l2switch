/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
import org.opendaylight.l2switch.packethandler.decoders.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.fields.ExtensionHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.fields.ExtensionHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6PacketBuilder;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
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
public class Ipv6Decoder extends AbstractPacketDecoder<EthernetPacketReceived, Ipv6PacketReceived>
  implements EthernetPacketListener {

  private static final Logger _logger = LoggerFactory.getLogger(Ipv6Decoder.class);

  public Ipv6Decoder(NotificationProviderService notificationProviderService) {
    super(Ipv6PacketReceived.class, notificationProviderService);
  }

  /**
   * Decode an EthernetPacket into an Ipv4Packet
   */
  @Override
  public Ipv6PacketReceived decode(EthernetPacketReceived ethernetPacketReceived) {
    Ipv6PacketReceivedBuilder ipv6ReceivedBuilder = new Ipv6PacketReceivedBuilder();

    // Find the latest packet in the packet-chain, which is an EthernetPacket
    List<PacketChain> packetChainList = ethernetPacketReceived.getPacketChain();
    EthernetPacket ethernetPacket = (EthernetPacket)packetChainList.get(packetChainList.size()-1).getPacket();
    int bitOffset = ethernetPacket.getPayloadOffset() * NetUtils.NumBitsInAByte;
    byte[] data = ethernetPacketReceived.getPayload();

    Ipv6PacketBuilder builder = new Ipv6PacketBuilder();
    try {
      builder.setVersion(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 4)));
      if (builder.getVersion().intValue() != 6) {
        _logger.debug("Version should be 6, but is " + builder.getVersion());
      }

      builder.setDscp(new Dscp(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 4, 6))));
      builder.setEcn(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 10, 2)));
      builder.setFlowLabel(BitBufferHelper.getLong(BitBufferHelper.getBits(data, bitOffset + 12, 20)));
      builder.setIpv6Length(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 32, 16)));
      builder.setNextHeader(KnownIpProtocols.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 48, 8))));
      builder.setHopLimit(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 56, 8)));
      builder.setSourceIpv6(Ipv6Address.getDefaultInstance(InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 64, 128)).getHostAddress()));
      builder.setDestinationIpv6(Ipv6Address.getDefaultInstance(InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 192, 128)).getHostAddress()));
      builder.setPayloadOffset((320 + bitOffset)/NetUtils.NumBitsInAByte);
      builder.setPayloadLength(builder.getIpv6Length());

      // Decode the optional "extension headers"
      List<ExtensionHeaders> extensionHeaders = new ArrayList<ExtensionHeaders>();
      KnownIpProtocols nextHeader = builder.getNextHeader();
      int extHeaderOffset = 0;
      while (nextHeader != null && !nextHeader.equals(KnownIpProtocols.Tcp) &&
        !nextHeader.equals(KnownIpProtocols.Udp)) {
        // Set the extension header's type & length & data
        short nextHeaderType = BitBufferHelper.getShort(BitBufferHelper.getBits(data, 320 + extHeaderOffset + bitOffset, 8));
        nextHeader = KnownIpProtocols.forValue(nextHeaderType);
        int octetLength = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 328 + extHeaderOffset + bitOffset, 8));
        int start = ( 336 + extHeaderOffset + bitOffset) / NetUtils.NumBitsInAByte;
        int end = start + 6 + octetLength;

        extensionHeaders.add(new ExtensionHeadersBuilder()
          .setNextHeader(nextHeader)
          .setLength(octetLength)
          .setData(Arrays.copyOfRange(data, start, end))
          .build());

        // Update the NextHeader field
        extHeaderOffset += 64 + octetLength * NetUtils.NumBitsInAByte;
      }
      if (!extensionHeaders.isEmpty()) {
        builder.setExtensionHeaders(extensionHeaders);
      }
    }
    catch (BufferException | UnknownHostException e) {
      _logger.debug("Exception while decoding IPv4 packet", e.getMessage());
    }

    //build ipv6
    packetChainList.add(new PacketChainBuilder()
      .setPacket(builder.build())
      .build());
    ipv6ReceivedBuilder.setPacketChain(packetChainList);

    // carry forward the original payload.
    ipv6ReceivedBuilder.setPayload(ethernetPacketReceived.getPayload());

    return ipv6ReceivedBuilder.build();
  }


  @Override
  public NotificationListener getConsumedNotificationListener() {
    return this;
  }

  @Override
  public void onEthernetPacketReceived(EthernetPacketReceived notification) {
    decodeAndPublish(notification);
  }

  @Override
  public boolean canDecode(EthernetPacketReceived ethernetPacketReceived) {
    if(ethernetPacketReceived==null || ethernetPacketReceived.getPacketChain()==null)
      return false;

    // Only decode the latest packet in the chain
    EthernetPacket ethernetPacket = null;
    if (!ethernetPacketReceived.getPacketChain().isEmpty()) {
      Packet packet = ethernetPacketReceived.getPacketChain().get(ethernetPacketReceived.getPacketChain().size()-1).getPacket();
      if (packet instanceof  EthernetPacket) {
        ethernetPacket = (EthernetPacket)packet;
      }
    }

    return ethernetPacket!=null && KnownEtherType.Ipv6.equals(ethernetPacket.getEthertype());
  }
}