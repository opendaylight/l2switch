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

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketOverRawReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketOverEthernetReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketOverEthernetReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.over.ethernet.fields.Ipv4PacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.over.ethernet.fields.EthernetOverRawPacketBuilder;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPv4 Packet Decoder
 */
public class Ipv4Decoder extends
    AbstractPacketDecoder<EthernetPacketOverRawReceived, Ipv4PacketOverEthernetReceived>
    implements EthernetPacketListener {

    private static final Logger _logger =
        LoggerFactory.getLogger(Ipv4Decoder.class);

    public Ipv4Decoder(
        NotificationProviderService notificationProviderService) {
        super(Ipv4PacketOverEthernetReceived.class,
            notificationProviderService);
    }

    /**
     * Decode an EthernetPacket into an Ipv4Packet
     */
    @Override
    public Ipv4PacketOverEthernetReceived decode(
        EthernetPacketOverRawReceived ethernetPacketOverRawReceived) {

        Ipv4PacketOverEthernetReceivedBuilder ipv4ReceivedBuilder =
            new Ipv4PacketOverEthernetReceivedBuilder();
        byte[] data = ethernetPacketOverRawReceived.getPayload();
        int offset =
            ethernetPacketOverRawReceived.getEthernetPacket().getPayloadOffset()
                * NetUtils.NumBitsInAByte;
        //TODO: PLease note that the payload is original payload and to decode ethernet payload use payload offset from ethernet

        Ipv4PacketBuilder builder = new Ipv4PacketBuilder();

        try {
            builder.setVersion(BitBufferHelper
                .getShort(BitBufferHelper.getBits(data, offset + 0, 4)));
            if (builder.getVersion().intValue() != 4) {
                _logger.debug(
                    "Version should be 4, but is " + builder.getVersion());
            }

            builder.setIhl(BitBufferHelper
                .getShort(BitBufferHelper.getBits(data, offset + 4, 4)));
            builder.setDscp(BitBufferHelper
                .getShort(BitBufferHelper.getBits(data, offset + 8, 6)));
            builder.setEcn(BitBufferHelper
                .getShort(BitBufferHelper.getBits(data, offset + 14, 2)));
            builder.setIpv4Length(BitBufferHelper
                .getInt(BitBufferHelper.getBits(data, offset + 16, 16)));
            builder.setId(BitBufferHelper
                .getInt(BitBufferHelper.getBits(data, offset + 32, 16)));

            // Decode the flags -- Reserved, DF (Don't Fragment), MF (More Fragments)
            builder.setReservedFlag(
                1 == (BitBufferHelper.getBits(data, offset + 48, 1)[0] & 0xff));
            if (builder.isReservedFlag()) {
                _logger.debug("Reserved flag should be 0, but is 1.");
            }
            // "& 0xff" removes the sign of the Java byte
            builder.setDfFlag(
                1 == (BitBufferHelper.getBits(data, offset + 49, 1)[0] & 0xff));
            builder.setMfFlag(
                1 == (BitBufferHelper.getBits(data, offset + 50, 1)[0] & 0xff));

            builder.setFragmentOffset(BitBufferHelper
                .getInt(BitBufferHelper.getBits(data, offset + 51, 13)));
            builder.setTtl(BitBufferHelper
                .getShort(BitBufferHelper.getBits(data, offset + 64, 8)));
            builder.setProtocol(KnownIpProtocols.forValue(BitBufferHelper
                .getShort(BitBufferHelper.getBits(data, offset + 72, 8))));
            builder.setChecksum(BitBufferHelper
                .getInt(BitBufferHelper.getBits(data, offset + 80, 16)));
            builder.setSourceIpv4(Ipv4Address.getDefaultInstance(InetAddress
                .getByAddress(BitBufferHelper.getBits(data, offset + 96, 32))
                .getHostAddress()));
            builder.setDestinationIpv4(Ipv4Address.getDefaultInstance(
                InetAddress.getByAddress(
                    BitBufferHelper.getBits(data, offset + 128, 32))
                    .getHostAddress()));

            // Decode the optional "options" parameter
            int optionsSize = (builder.getIhl() - 5) * 32;
            if (optionsSize > 0) {
                builder.setIpv4Options(
                    BitBufferHelper.getBits(data, offset + 160, optionsSize));
            }

            // Decode the IPv4 Payload -- Not sure if needed now
            int payloadStartInBits = offset + 160 + optionsSize;
            int payloadEndInBits =
                data.length * NetUtils.NumBitsInAByte - payloadStartInBits
                    - 4 * NetUtils.NumBitsInAByte;
            int start = payloadStartInBits / NetUtils.NumBitsInAByte;
            int end = start + payloadEndInBits / NetUtils.NumBitsInAByte;
            builder.setPayloadOffset(start);
            builder.setPayloadLength(end - start);


            ipv4ReceivedBuilder.setIpv4Packet(builder.build());
            ipv4ReceivedBuilder
                .setPayload(ethernetPacketOverRawReceived.getPayload());

            EthernetOverRawPacketBuilder ethernetOverRawPacketBuilder =
                new EthernetOverRawPacketBuilder();
            ethernetOverRawPacketBuilder
                .setRawPacket(ethernetPacketOverRawReceived.getRawPacket());
            ethernetOverRawPacketBuilder.setEthernetPacket(
                ethernetPacketOverRawReceived.getEthernetPacket());
            ipv4ReceivedBuilder
                .setEthernetOverRawPacket(ethernetOverRawPacketBuilder.build());

        } catch (BufferException | UnknownHostException e) {
            _logger
                .debug("Exception while decoding IPv4 packet", e.getMessage());
        }
        return ipv4ReceivedBuilder.build();

    }


    @Override
    public NotificationListener getConsumedNotificationListener() {
        return this;
    }

    @Override
    public void onEthernetPacketOverRawReceived(
        EthernetPacketOverRawReceived notification) {
        decodeAndPublish(notification);
    }

    @Override
    public boolean canDecode(
        EthernetPacketOverRawReceived ethernetPacketOverRawReceived) {
        if (ethernetPacketOverRawReceived == null
            || ethernetPacketOverRawReceived.getEthernetPacket() == null)
            return false;

        return KnownEtherType.Ipv4.equals(
            ethernetPacketOverRawReceived.getEthernetPacket().getEthertype());
    }
}
