/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import org.opendaylight.mdsal.binding.api.NotificationService.Listener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.raw.packet.RawPacketFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6Packet;

/**
 * AddressObserver listens to IPv6 packets to find addresses (mac, ip) and store
 * these address observations for each node-connector. These packets are
 * returned to the network after the addresses are learned.
 */
public class AddressObserverUsingIpv6 implements Listener<Ipv6PacketReceived> {
    private static final String IPV6_IP_TO_IGNORE = "0:0:0:0:0:0:0:0";

    private final AddressObservationWriter addressObservationWriter;

    public AddressObserverUsingIpv6(AddressObservationWriter addressObservationWriter) {
        this.addressObservationWriter = addressObservationWriter;
    }

    /**
     * The handler function for IPv6 packets.
     *
     * @param packetReceived
     *            The incoming packet.
     */
    @Override
    public void onNotification(Ipv6PacketReceived packetReceived) {
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }

        RawPacketFields rawPacket = null;
        EthernetPacket ethernetPacket = null;
        Ipv6Packet ipv6Packet = null;
        for (PacketChain packetChain : packetReceived.getPacketChain()) {
            if (packetChain.getPacket() instanceof RawPacket) {
                rawPacket = ((RawPacket) packetChain.getPacket()).getRawPacketFields();
            } else if (packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof Ipv6Packet) {
                ipv6Packet = (Ipv6Packet) packetChain.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || ipv6Packet == null) {
            return;
        }

        if (!IPV6_IP_TO_IGNORE.equals(ipv6Packet.getSourceIpv6().getValue())) {
            addressObservationWriter.addAddress(ethernetPacket.getSourceMac(),
                IetfInetUtil.ipAddressFor(ipv6Packet.getSourceIpv6().getValue()), rawPacket.getIngress());
        }
    }
}
