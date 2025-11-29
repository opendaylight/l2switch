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
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.l2switch.packethandler.SubsequentDecoder;
import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4PacketBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPv4 Packet Decoder.
 */
public final class Ipv4Decoder extends SubsequentDecoder<EthernetPacketReceived, Ipv4PacketReceived> {
    private static final Logger LOG = LoggerFactory.getLogger(Ipv4Decoder.class);

    public Ipv4Decoder() {
        super(EthernetPacketReceived.class, Ipv4PacketReceived.class);
    }

    /**
     * Decode an EthernetPacket into an Ipv4Packet.
     */
    @Override
    protected Ipv4PacketReceived tryDecode(final EthernetPacketReceived input, final List<PacketChain> chain) {
        // Find the latest packet in the packet-chain, which is an EthernetPacket
        if (!(chain.getLast().getPacket() instanceof EthernetPacket ethernetPacket)
            || !KnownEtherType.Ipv4.equals(ethernetPacket.getEthertype())) {
            return null;
        }

        int bitOffset = ethernetPacket.getPayloadOffset().intValue() * Byte.SIZE;
        byte[] data = input.getPayload();

        Ipv4PacketBuilder builder = new Ipv4PacketBuilder();
        try {
            builder.setVersion(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset, 4)));
            if (builder.getVersion().intValue() != 4) {
                LOG.debug("Version should be 4, but is {}", builder.getVersion());
            }

            builder.setIhl(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 4, 4)));
            builder.setDscp(new Dscp(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 8, 6))));
            builder.setEcn(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 14, 2)));
            builder.setIpv4Length(BitBufferHelper.getUint16(BitBufferHelper.getBits(data, bitOffset + 16, 16)));
            builder.setId(BitBufferHelper.getUint16(BitBufferHelper.getBits(data, bitOffset + 32, 16)));

            // Decode the flags -- Reserved, DF (Don't Fragment), MF (More Fragments)
            builder.setReservedFlag(1 == (BitBufferHelper.getBits(data, bitOffset + 48, 1)[0] & 0xff));
            if (builder.getReservedFlag()) {
                LOG.debug("Reserved flag should be 0, but is 1.");
            }
            // "& 0xff" removes the sign of the Java byte
            builder.setDfFlag(1 == (BitBufferHelper.getBits(data, bitOffset + 49, 1)[0] & 0xff));
            builder.setMfFlag(1 == (BitBufferHelper.getBits(data, bitOffset + 50, 1)[0] & 0xff));

            builder.setFragmentOffset(BitBufferHelper.getUint16(BitBufferHelper.getBits(data, bitOffset + 51, 13)));
            builder.setTtl(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 64, 8)));
            builder.setProtocol(KnownIpProtocols
                    .forValue(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset + 72, 8))));
            builder.setChecksum(BitBufferHelper.getUint16(BitBufferHelper.getBits(data, bitOffset + 80, 16)));
            builder.setSourceIpv4(Ipv4Address.getDefaultInstance(
                    InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 96, 32)).getHostAddress()));
            builder.setDestinationIpv4(Ipv4Address.getDefaultInstance(
                    InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 128, 32)).getHostAddress()));

            // Decode the optional "options" parameter
            int optionsSize = (builder.getIhl().toJava() - 5) * 32;
            if (optionsSize > 0) {
                builder.setIpv4Options(BitBufferHelper.getBits(data, bitOffset + 160, optionsSize));
            }

            // Decode the IPv4 Payload
            int payloadStartInBits = bitOffset + 160 + optionsSize;
            int payloadEndInBits = data.length * Byte.SIZE - payloadStartInBits - 4 * Byte.SIZE;
            int start = payloadStartInBits / Byte.SIZE;
            int end = start + payloadEndInBits / Byte.SIZE;
            builder.setPayloadOffset(Uint32.valueOf(start));
            builder.setPayloadLength(Uint32.valueOf(end - start));
        } catch (BufferException | UnknownHostException e) {
            LOG.debug("Exception while decoding IPv4 packet", e);
        }

        // build ipv4
        final var packetChain = new ArrayList<PacketChain>(chain.size() + 1);
        packetChain.addAll(chain);
        packetChain.add(new PacketChainBuilder().setPacket(builder.build()).build());

        return new Ipv4PacketReceivedBuilder()
            .setPacketChain(packetChain)
            // carry forward the original payload.
            .setPayload(input.getPayload())
            .build();
    }
}
