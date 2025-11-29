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
import java.util.Arrays;
import java.util.List;
import org.opendaylight.l2switch.packethandler.SubsequentDecoder;
import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.fields.ExtensionHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.fields.ExtensionHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6PacketBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPv6 Packet Decoder.
 */
public final class Ipv6Decoder extends SubsequentDecoder<EthernetPacketReceived, Ipv6PacketReceived> {
    private static final Logger LOG = LoggerFactory.getLogger(Ipv6Decoder.class);

    public Ipv6Decoder() {
        super(EthernetPacketReceived.class, Ipv6PacketReceived.class);
    }

    /**
     * Decode an EthernetPacket into an Ipv4Packet.
     */
    @Override
    protected Ipv6PacketReceived decode(final EthernetPacketReceived ethernetPacketReceived) {
        // Find the latest packet in the packet-chain, which is an
        // EthernetPacket
        final var recvPacketChain = ethernetPacketReceived.nonnullPacketChain();
        var ethernetPacket = (EthernetPacket) recvPacketChain.getLast().getPacket();
        int bitOffset = ethernetPacket.getPayloadOffset().intValue() * Byte.SIZE;
        byte[] data = ethernetPacketReceived.getPayload();

        Ipv6PacketBuilder builder = new Ipv6PacketBuilder();
        try {
            builder.setVersion(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset, 4)));
            if (builder.getVersion().intValue() != 6) {
                LOG.debug("Version should be 6, but is {}", builder.getVersion());
            }

            builder.setDscp(new Dscp(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 4, 6))));
            builder.setEcn(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 10, 2)));
            builder.setFlowLabel(BitBufferHelper.getUint32(BitBufferHelper.getBits(data, bitOffset + 12, 20)));
            builder.setIpv6Length(BitBufferHelper.getUint16(BitBufferHelper.getBits(data, bitOffset + 32, 16)));
            builder.setNextHeader(KnownIpProtocols
                    .forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset + 48, 8))));
            builder.setHopLimit(BitBufferHelper.getUint8(BitBufferHelper.getBits(data, bitOffset + 56, 8)));
            builder.setSourceIpv6(Ipv6Address.getDefaultInstance(
                    InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 64, 128)).getHostAddress()));
            builder.setDestinationIpv6(Ipv6Address.getDefaultInstance(
                    InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 192, 128)).getHostAddress()));
            builder.setPayloadOffset(Uint32.valueOf((320 + bitOffset) / Byte.SIZE));
            builder.setPayloadLength(builder.getIpv6Length().toUint32());

            // Decode the optional "extension headers"
            List<ExtensionHeaders> extensionHeaders = new ArrayList<>();
            KnownIpProtocols nextHeader = builder.getNextHeader();
            int extHeaderOffset = 0;
            while (nextHeader != null && !nextHeader.equals(KnownIpProtocols.Tcp)
                    && !nextHeader.equals(KnownIpProtocols.Udp)) {
                // Set the extension header's type & length & data
                short nextHeaderType = BitBufferHelper
                        .getShort(BitBufferHelper.getBits(data, 320 + extHeaderOffset + bitOffset, 8));
                nextHeader = KnownIpProtocols.forValue(nextHeaderType);
                int octetLength = BitBufferHelper
                        .getInt(BitBufferHelper.getBits(data, 328 + extHeaderOffset + bitOffset, 8));
                int start = (336 + extHeaderOffset + bitOffset) / Byte.SIZE;
                int end = start + 6 + octetLength;

                extensionHeaders.add(new ExtensionHeadersBuilder()
                        .setNextHeader(nextHeader)
                        .setLength(Uint16.valueOf(octetLength))
                        .setData(Arrays.copyOfRange(data, start, end))
                        .build());

                // Update the NextHeader field
                extHeaderOffset += 64 + octetLength * Byte.SIZE;
            }
            if (!extensionHeaders.isEmpty()) {
                builder.setExtensionHeaders(extensionHeaders);
            }
        } catch (BufferException | UnknownHostException e) {
            LOG.debug("Exception while decoding IPv6 packet", e);
        }

        // build ipv6
        final var packetChain = new ArrayList<PacketChain>(recvPacketChain.size());
        packetChain.addAll(recvPacketChain);
        packetChain.add(new PacketChainBuilder().setPacket(builder.build()).build());

        return new Ipv6PacketReceivedBuilder()
            .setPacketChain(packetChain)
            // carry forward the original payload.
            .setPayload(ethernetPacketReceived.getPayload())
            .build();
    }

    @Override
    protected boolean canDecode(final EthernetPacketReceived ethernetPacketReceived) {
        final var packetChain = ethernetPacketReceived.getPacketChain();
        return packetChain != null && !packetChain.isEmpty()
            && packetChain.getLast().getPacket() instanceof EthernetPacket ethernetPacket
            && KnownEtherType.Ipv6.equals(ethernetPacket.getEthertype());
    }
}
