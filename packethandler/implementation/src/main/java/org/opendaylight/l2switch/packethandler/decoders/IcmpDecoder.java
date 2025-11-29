/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import java.util.List;
import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.NotificationService.Listener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.icmp.rev140528.IcmpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.icmp.rev140528.IcmpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.icmp.rev140528.icmp.packet.received.packet.chain.packet.IcmpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcmpDecoder extends AbstractPacketDecoder<Ipv4PacketReceived, IcmpPacketReceived>
        implements Listener<Ipv4PacketReceived> {

    private static final Logger LOG = LoggerFactory.getLogger(IcmpDecoder.class);

    public IcmpDecoder(NotificationPublishService notificationProviderService,
                       NotificationService notificationService) {
        super(IcmpPacketReceived.class, notificationProviderService, notificationService);
    }

    /**
     * Decode an EthernetPacket into an IcmpPacket.
     */
    @Override
    public IcmpPacketReceived decode(Ipv4PacketReceived ipv4PacketReceived) {
        IcmpPacketReceivedBuilder icmpReceivedBuilder = new IcmpPacketReceivedBuilder();

        // Find the latest packet in the packet-chain, which is an
        // EthernetPacket
        List<PacketChain> packetChainList = ipv4PacketReceived.getPacketChain();
        Ipv4Packet ipv4Packet = (Ipv4Packet) packetChainList.get(packetChainList.size() - 1).getPacket();
        int bitOffset = ipv4Packet.getPayloadOffset().intValue() * Byte.SIZE;
        byte[] data = ipv4PacketReceived.getPayload();

        IcmpPacketBuilder builder = new IcmpPacketBuilder();
        try {
            // Decode the ICMP type and ICMP code
            builder.setType(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 0, 8)));
            builder.setCode(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 8, 8)));

            // Decode the checksum
            builder.setCrc(BitBufferHelper.getUint16(BitBufferHelper.getBits(data, bitOffset + 16, 16)));

            // Decode the identifier and sequence number
            builder.setIdentifier(BitBufferHelper.getUint16(BitBufferHelper.getBits(data, bitOffset + 32, 16)));
            builder.setSequenceNumber(BitBufferHelper.getUint16(BitBufferHelper.getBits(data, bitOffset + 48, 16)));

            // Decode the ICMP Payload
            int payloadStartInBits = bitOffset + 64;
            int payloadEndInBits = data.length * Byte.SIZE - payloadStartInBits - 4 * Byte.SIZE;
            int start = payloadStartInBits / Byte.SIZE;
            int end = start + payloadEndInBits / Byte.SIZE;
            builder.setPayloadOffset(Uint32.valueOf(start));
            builder.setPayloadLength(Uint32.valueOf(end - start));
        } catch (BufferException e) {
            LOG.debug("Exception while decoding ICMP packet", e);
        }

        // build icmp
        packetChainList.add(new PacketChainBuilder().setPacket(builder.build()).build());
        icmpReceivedBuilder.setPacketChain(packetChainList);

        // carry forward the original payload.
        icmpReceivedBuilder.setPayload(ipv4PacketReceived.getPayload());

        return icmpReceivedBuilder.build();
    }

    @Override
    public Listener<Ipv4PacketReceived> getConsumedListener() {
        return this;
    }

    @Override
    public Class<IcmpPacketReceived> getPacketType() {
        return IcmpPacketReceived.class;
    }

    @Override
    public void onNotification(Ipv4PacketReceived notification) {
        decodeAndPublish(notification);
    }

    @Override
    public boolean canDecode(Ipv4PacketReceived ipv4PacketReceived) {
        if (ipv4PacketReceived == null || ipv4PacketReceived.getPacketChain() == null) {
            return false;
        }

        // Only decode the latest packet in the chain
        Ipv4Packet ipv4Packet = null;
        if (!ipv4PacketReceived.getPacketChain().isEmpty()) {
            Packet packet = ipv4PacketReceived.getPacketChain().get(ipv4PacketReceived.getPacketChain().size() - 1)
                    .getPacket();
            if (packet instanceof Ipv4Packet) {
                ipv4Packet = (Ipv4Packet) packet;
            }
        }

        return ipv4Packet != null && KnownIpProtocols.Icmp.equals(ipv4Packet.getProtocol());
    }
}
