/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
import org.opendaylight.l2switch.packethandler.decoders.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4PacketBuilder;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPv4 Packet Decoder
 */
public class Ipv4Decoder extends AbstractPacketDecoder<EthernetPacketReceived, Ipv4PacketReceived>
    implements EthernetPacketListener {

  private static final Logger _logger = LoggerFactory.getLogger(Ipv4Decoder.class);

  public Ipv4Decoder(NotificationProviderService notificationProviderService) {
    super(Ipv4PacketReceived.class, notificationProviderService);
  }

  /**
   * Decode an EthernetPacket into an Ipv4Packet
   */
  @Override
  public Ipv4PacketReceived decode(EthernetPacketReceived ethernetPacketReceived) {
    Ipv4PacketReceivedBuilder ipv4ReceivedBuilder = new Ipv4PacketReceivedBuilder();

    // Find the latest packet in the packet-chain, which is an EthernetPacket
    List<PacketChain> packetChainList = ethernetPacketReceived.getPacketChain();
    EthernetPacket ethernetPacket = (EthernetPacket)packetChainList.get(packetChainList.size()-1).getPacket();
    int bitOffset = ethernetPacket.getPayloadOffset() * NetUtils.NumBitsInAByte;
    byte[] data = ethernetPacketReceived.getPayload();

    Ipv4PacketBuilder builder = new Ipv4PacketBuilder();
    try {
      builder.setVersion(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 4)));
      if (builder.getVersion().intValue() != 4) {
        _logger.debug("Version should be 4, but is " + builder.getVersion());
      }

      builder.setIhl(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 4, 4)));
      builder.setDscp(new Dscp(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 8, 6))));
      builder.setEcn(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 14, 2)));
      builder.setIpv4Length(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 16, 16)));
      builder.setId(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 32, 16)));

      // Decode the flags -- Reserved, DF (Don't Fragment), MF (More Fragments)
      builder.setReservedFlag(1 == (BitBufferHelper.getBits(data, bitOffset + 48, 1)[0] & 0xff));
      if (builder.isReservedFlag()) {
        _logger.debug("Reserved flag should be 0, but is 1.");
      }
      // "& 0xff" removes the sign of the Java byte
      builder.setDfFlag(1 == (BitBufferHelper.getBits(data, bitOffset + 49, 1)[0] & 0xff));
      builder.setMfFlag(1 == (BitBufferHelper.getBits(data, bitOffset + 50, 1)[0] & 0xff));

      builder.setFragmentOffset(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 51, 13)));
      builder.setTtl(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 64, 8)));
      builder.setProtocol(KnownIpProtocols.forValue(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 72, 8))));
      builder.setChecksum(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 80, 16)));
      builder.setSourceIpv4(Ipv4Address.getDefaultInstance(
        InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 96, 32))
        .getHostAddress()));
      builder.setDestinationIpv4(Ipv4Address.getDefaultInstance(
        InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 128, 32))
        .getHostAddress()));

      // Decode the optional "options" parameter
      int optionsSize = (builder.getIhl() - 5) * 32;
      if (optionsSize > 0) {
        builder.setIpv4Options(BitBufferHelper.getBits(data, bitOffset + 160, optionsSize));
      }

      // Decode the IPv4 Payload
      int payloadStartInBits = bitOffset + 160 + optionsSize;
      int payloadEndInBits = data.length * NetUtils.NumBitsInAByte - payloadStartInBits - 4 * NetUtils.NumBitsInAByte;
      int start = payloadStartInBits / NetUtils.NumBitsInAByte;
      int end = start + payloadEndInBits / NetUtils.NumBitsInAByte;
      builder.setPayloadOffset(start);
      builder.setPayloadLength(end - start);
    } catch (BufferException | UnknownHostException e) {
        _logger
            .debug("Exception while decoding IPv4 packet", e.getMessage());
    }

    //build ipv4
    packetChainList.add(new PacketChainBuilder()
      .setPacket(builder.build())
      .build());
    ipv4ReceivedBuilder.setPacketChain(packetChainList);

    // carry forward the original payload.
    ipv4ReceivedBuilder.setPayload(ethernetPacketReceived.getPayload());

    return ipv4ReceivedBuilder.build();
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

    return ethernetPacket!=null && KnownEtherType.Ipv4.equals(ethernetPacket.getEthertype());
  }
}
