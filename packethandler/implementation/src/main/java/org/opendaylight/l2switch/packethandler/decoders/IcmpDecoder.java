/*
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import java.util.List;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
import org.opendaylight.l2switch.packethandler.decoders.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.icmp.rev140528.IcmpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.icmp.rev140528.IcmpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.icmp.rev140528.icmp.packet.received.packet.chain.packet.IcmpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcmpDecoder extends AbstractPacketDecoder<Ipv4PacketReceived, IcmpPacketReceived>
        implements Ipv4PacketListener {

    private static final Logger LOG = LoggerFactory.getLogger(IcmpDecoder.class);

    public IcmpDecoder(NotificationProviderService notificationProviderService) {
        super(IcmpPacketReceived.class, notificationProviderService);
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
        int bitOffset = ipv4Packet.getPayloadOffset() * NetUtils.NUM_BITS_IN_A_BYTE;
        byte[] data = ipv4PacketReceived.getPayload();

        IcmpPacketBuilder builder = new IcmpPacketBuilder();
        try {
            // Decode the ICMP type and ICMP code
            builder.setType(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 0, 8)));
            builder.setCode(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 8, 8)));

            // Decode the checksum
            builder.setCrc(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 16, 16)));

            // Decode the identifier and sequence number
            builder.setIdentifier(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 32, 16)));
            builder.setSequenceNumber(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 48, 16)));

            // Decode the ICMP Payload
            int payloadStartInBits = bitOffset + 64;
            int payloadEndInBits = data.length * NetUtils.NUM_BITS_IN_A_BYTE - payloadStartInBits - 4
                    * NetUtils.NUM_BITS_IN_A_BYTE;
            int start = payloadStartInBits / NetUtils.NUM_BITS_IN_A_BYTE;
            int end = start + payloadEndInBits / NetUtils.NUM_BITS_IN_A_BYTE;
            builder.setPayloadOffset(start);
            builder.setPayloadLength(end - start);
        } catch (BufferException e) {
            LOG.debug("Exception while decoding ICMP packet", e.getMessage());
        }

        // build icmp
        packetChainList.add(new PacketChainBuilder().setPacket(builder.build()).build());
        icmpReceivedBuilder.setPacketChain(packetChainList);

        // carry forward the original payload.
        icmpReceivedBuilder.setPayload(ipv4PacketReceived.getPayload());

        return icmpReceivedBuilder.build();
    }

    @Override
    public NotificationListener getConsumedNotificationListener() {
        return this;
    }

    @Override
    public void onIpv4PacketReceived(Ipv4PacketReceived notification) {
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
