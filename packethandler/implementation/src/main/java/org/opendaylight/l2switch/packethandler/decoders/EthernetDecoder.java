/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.packethandler.decoders;

import java.util.ArrayList;
import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
import org.opendaylight.l2switch.packethandler.decoders.utils.HexEncode;
import org.opendaylight.l2switch.packethandler.decoders.utils.NetUtils;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.raw.packet.fields.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.Header8021qType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.fields.Header8021q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.fields.Header8021qBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ethernet Packet Decoder.
 */
public class EthernetDecoder extends AbstractPacketDecoder<PacketReceived, EthernetPacketReceived>
        implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(EthernetDecoder.class);
    public static final Integer LENGTH_MAX = 1500;
    public static final Integer ETHERTYPE_MIN = 1536;
    public static final Integer ETHERTYPE_8021Q = 0x8100;
    public static final Integer ETHERTYPE_QINQ = 0x9100;

    public EthernetDecoder(NotificationPublishService notificationProviderService,
                           NotificationService notificationService) {
        super(EthernetPacketReceived.class, notificationProviderService, notificationService);
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        decodeAndPublish(packetReceived);
    }

    /**
     * Decode a RawPacket into an EthernetPacket.
     *
     * @param packetReceived
     *            -- data from wire to deserialize
     * @return EthernetPacketReceived
     */
    @Override
    public EthernetPacketReceived decode(PacketReceived packetReceived) {
        byte[] data = packetReceived.getPayload();
        EthernetPacketReceivedBuilder builder = new EthernetPacketReceivedBuilder();

        // Save original rawPacket & set the payloadOffset/payloadLength fields
        RawPacketBuilder rpb = new RawPacketBuilder().setIngress(packetReceived.getIngress())
                .setConnectionCookie(packetReceived.getConnectionCookie()).setFlowCookie(packetReceived.getFlowCookie())
                .setTableId(packetReceived.getTableId()).setPacketInReason(packetReceived.getPacketInReason())
                .setPayloadOffset(0).setPayloadLength(data.length);
        if (packetReceived.getMatch() != null) {
            rpb.setMatch(new MatchBuilder(packetReceived.getMatch()).build());
        }
        RawPacket rp = rpb.build();
        ArrayList<PacketChain> packetChain = new ArrayList<>();
        packetChain.add(new PacketChainBuilder().setPacket(rp).build());

        try {
            EthernetPacketBuilder epBuilder = new EthernetPacketBuilder();

            // Deserialize the destination & source fields
            epBuilder.setDestinationMac(
                        new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 0, 48))));
            epBuilder.setSourceMac(
                        new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 48, 48))));

            // Deserialize the optional field 802.1Q headers
            Integer nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 96, 16));
            int extraHeaderBits = 0;
            ArrayList<Header8021q> headerList = new ArrayList<>();
            while (nextField.equals(ETHERTYPE_8021Q) || nextField.equals(ETHERTYPE_QINQ)) {
                Header8021qBuilder headerBuilder = new Header8021qBuilder();
                headerBuilder.setTPID(Header8021qType.forValue(nextField));

                // Read 2 more bytes for priority (3bits), drop eligible (1bit),
                // vlan-id (12bits)
                byte[] vlanBytes = BitBufferHelper.getBits(data, 112 + extraHeaderBits, 16);

                // Remove the sign & right-shift to get the priority code
                headerBuilder.setPriorityCode((short) ((vlanBytes[0] & 0xff) >> 5));

                // Remove the sign & remove priority code bits & right-shift to
                // get drop-eligible bit
                headerBuilder.setDropEligible(1 == (vlanBytes[0] & 0xff & 0x10) >> 4);

                // Remove priority code & drop-eligible bits, to get the VLAN-id
                vlanBytes[0] = (byte) (vlanBytes[0] & 0x0F);
                headerBuilder.setVlan(new VlanId(BitBufferHelper.getInt(vlanBytes)));

                // Add 802.1Q header to the growing collection
                headerList.add(headerBuilder.build());

                // Reset value of "nextField" to correspond to following 2 bytes
                // for next 802.1Q header or EtherType/Length
                nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 128 + extraHeaderBits, 16));

                // 802.1Q header means payload starts at a later position
                extraHeaderBits += 32;
            }
            // Set 802.1Q headers
            if (!headerList.isEmpty()) {
                epBuilder.setHeader8021q(headerList);
            }

            // Deserialize the EtherType or Length field
            if (nextField >= ETHERTYPE_MIN) {
                epBuilder.setEthertype(KnownEtherType.forValue(nextField));
            } else if (nextField <= LENGTH_MAX) {
                epBuilder.setEthernetLength(nextField);
            } else {
                LOG.debug("Undefined header, value is not valid EtherType or length.  Value is {}", nextField);
            }

            // Determine start & end of payload
            int payloadStart = (112 + extraHeaderBits) / NetUtils.NUM_BITS_IN_A_BYTE;
            int payloadEnd = data.length - 4;
            epBuilder.setEthPayloadOffset(payloadStart);
            epBuilder.setEthPayloadLength(payloadEnd - payloadStart);

            // Deserialize the CRC
            epBuilder.setCrc(BitBufferHelper
                    .getLong(BitBufferHelper.getBits(data, (data.length - 4) * NetUtils.NUM_BITS_IN_A_BYTE, 32)));

            // Set EthernetPacket field
            packetChain.add(new PacketChainBuilder().setPacket(epBuilder.build()).build());

            // Set Payload field
            builder.setPayload(data);
        } catch (BufferException be) {
            LOG.info("Exception during decoding raw packet to ethernet.");
        }

        // ToDo: Possibly log these values
        /*
         * if (_logger.isTraceEnabled()) {
         * _logger.trace("{}: {}: {} (offset {} bitsize {})", new Object[] {
         * this.getClass().getSimpleName(), hdrField,
         * HexEncode.bytesToHexString(hdrFieldBytes), startOffset, numBits }); }
         */
        builder.setPacketChain(packetChain);
        return builder.build();
    }

    @Override
    public NotificationListener getConsumedNotificationListener() {
        return this;
    }

    @Override
    public boolean canDecode(PacketReceived packetReceived) {
        return packetReceived != null && packetReceived.getPayload() != null;
    }
}
