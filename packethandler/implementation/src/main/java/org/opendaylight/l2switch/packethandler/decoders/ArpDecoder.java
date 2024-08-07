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
import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
import org.opendaylight.l2switch.packethandler.decoders.utils.HexEncode;
import org.opendaylight.l2switch.packethandler.decoders.utils.NetUtils;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.NotificationService.Listener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownHardwareType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ARP (Address Resolution Protocol) Packet Decoder.
 */
public class ArpDecoder extends AbstractPacketDecoder<EthernetPacketReceived, ArpPacketReceived>
        implements Listener<EthernetPacketReceived> {
    private static final Logger LOG = LoggerFactory.getLogger(ArpDecoder.class);

    public ArpDecoder(NotificationPublishService notificationProviderService, NotificationService notificationService) {
        super(ArpPacketReceived.class, notificationProviderService, notificationService);
    }

    /**
     * Decode an EthernetPacket into an ArpPacket.
     */
    @Override
    public ArpPacketReceived decode(EthernetPacketReceived ethernetPacketReceived) {
        ArpPacketReceivedBuilder arpReceivedBuilder = new ArpPacketReceivedBuilder();

        // Find the latest packet in the packet-chain, which is an
        // EthernetPacket
        List<PacketChain> packetChainList = ethernetPacketReceived.getPacketChain();
        EthernetPacket ethernetPacket = (EthernetPacket) packetChainList.get(packetChainList.size() - 1).getPacket();
        int bitOffset = ethernetPacket.getPayloadOffset().intValue() * NetUtils.NUM_BITS_IN_A_BYTE;
        byte[] data = ethernetPacketReceived.getPayload();

        ArpPacketBuilder builder = new ArpPacketBuilder();
        try {
            // Decode the hardware-type (HTYPE) and protocol-type (PTYPE) fields
            builder.setHardwareType(KnownHardwareType
                    .forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 0, 16))));
            builder.setProtocolType(
                    KnownEtherType.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 16, 16))));

            // Decode the hardware-length and protocol-length fields
            builder.setHardwareLength(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 32, 8)));
            builder.setProtocolLength(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 40, 8)));

            // Decode the operation field
            builder.setOperation(
                    KnownOperation.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 48, 16))));

            // Decode the address fields
            int indexSrcProtAdd = 64 + 8 * builder.getHardwareLength().toJava();
            int indexDstHardAdd = indexSrcProtAdd + 8 * builder.getProtocolLength().toJava();
            int indexDstProtAdd = indexDstHardAdd + 8 * builder.getHardwareLength().toJava();
            if (builder.getHardwareType().equals(KnownHardwareType.Ethernet)) {
                builder.setSourceHardwareAddress(HexEncode.bytesToHexStringFormat(
                    BitBufferHelper.getBits(data, bitOffset + 64, 8 * builder.getHardwareLength().toJava())));
                builder.setDestinationHardwareAddress(HexEncode.bytesToHexStringFormat(
                    BitBufferHelper.getBits(data, bitOffset + indexDstHardAdd,
                        8 * builder.getHardwareLength().toJava())));
            } else {
                LOG.debug("Unknown HardwareType -- source and destination  HardwareAddress are not decoded");
            }

            if (builder.getProtocolType().equals(KnownEtherType.Ipv4)
                    || builder.getProtocolType().equals(KnownEtherType.Ipv6)) {
                builder.setSourceProtocolAddress(InetAddress.getByAddress(
                    BitBufferHelper.getBits(data,bitOffset + indexSrcProtAdd, 8 * builder.getProtocolLength().toJava()))
                    .getHostAddress());
                builder.setDestinationProtocolAddress(InetAddress.getByAddress(
                    BitBufferHelper.getBits(data, bitOffset + indexDstProtAdd,
                        8 * builder.getProtocolLength().toJava())).getHostAddress());
            } else {
                LOG.debug("Unknown ProtocolType -- source and destination ProtocolAddress are not decoded");
            }
        } catch (BufferException | UnknownHostException e) {
            LOG.debug("Exception while decoding APR packet", e);
        }

        // build arp
        packetChainList.add(new PacketChainBuilder().setPacket(builder.build()).build());
        arpReceivedBuilder.setPacketChain(packetChainList);

        // carry forward the original payload.
        arpReceivedBuilder.setPayload(ethernetPacketReceived.getPayload());
        return arpReceivedBuilder.build();
    }

    @Override
    public Listener<EthernetPacketReceived> getConsumedListener() {
        return this;
    }

    @Override
    public Class<ArpPacketReceived> getPacketType() {
        return ArpPacketReceived.class;
    }

    @Override
    public void onNotification(EthernetPacketReceived notification) {
        decodeAndPublish(notification);
    }

    @Override
    public boolean canDecode(EthernetPacketReceived ethernetPacketReceived) {
        if (ethernetPacketReceived == null || ethernetPacketReceived.getPacketChain() == null) {
            return false;
        }

        // Only decode the latest packet in the chain
        EthernetPacket ethernetPacket = null;
        if (!ethernetPacketReceived.getPacketChain().isEmpty()) {
            Packet packet = ethernetPacketReceived.getPacketChain()
                    .get(ethernetPacketReceived.getPacketChain().size() - 1).getPacket();
            if (packet instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packet;
            }
        }

        return ethernetPacket != null && KnownEtherType.Arp.equals(ethernetPacket.getEthertype());
    }
}
