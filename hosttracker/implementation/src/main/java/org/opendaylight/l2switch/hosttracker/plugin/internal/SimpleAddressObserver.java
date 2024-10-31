/*
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import java.math.BigInteger;
import java.util.Date;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.raw.packet.RawPacketFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6Packet;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * A Simple Address Observer based on l2switch address observer.
 */
public class SimpleAddressObserver {
    private static final String IPV4_IP_TO_IGNORE = "0.0.0.0";
    private static final String IPV6_IP_TO_IGNORE = "0:0:0:0:0:0:0:0";

    private final HostTrackerImpl hostTrackerImpl;
    private final NotificationService notificationService;

    public SimpleAddressObserver(final HostTrackerImpl hostTrackerImpl, final NotificationService notificationService) {
        this.hostTrackerImpl = hostTrackerImpl;
        this.notificationService = notificationService;
    }

    Registration registerAsNotificationListener() {
        return notificationService.registerCompositeListener(new CompositeListener(Set.of(
            new CompositeListener.Component<>(ArpPacketReceived.class, this::onArpPacketReceived),
            new CompositeListener.Component<>(Ipv4PacketReceived.class, this::onIpv4PacketReceived),
            new CompositeListener.Component<>(Ipv6PacketReceived.class, this::onIpv6PacketReceived))));
    }

    @NonNullByDefault
    private void onArpPacketReceived(final ArpPacketReceived packetReceived) {
        RawPacketFields rawPacket = null;
        EthernetPacket ethernetPacket = null;
        ArpPacket arpPacket = null;
        for (var packetChain : packetReceived.nonnullPacketChain()) {
            // TODO: use an enhanced switch when we have Java 21
            final var packet = packetChain.getPacket();
            if (packet instanceof RawPacket raw) {
                rawPacket = raw.getRawPacketFields();
            } else if (packet instanceof EthernetPacket ethernet) {
                ethernetPacket = ethernet;
            } else if (packet instanceof ArpPacket arp) {
                arpPacket = arp;
            }
        }
        if (rawPacket == null || ethernetPacket == null || arpPacket == null) {
            return;
        }

        final VlanId vlanId;
        if (ethernetPacket.getEthertype().equals(KnownEtherType.VlanTagged)) {
            vlanId = ethernetPacket.getHeader8021q().get(0).getVlan();
        } else {
            vlanId = null;
        }

        final IpAddress ipAddress;
        if (arpPacket.getProtocolType().equals(KnownEtherType.Ipv4)) {
            ipAddress = new IpAddress(new Ipv4Address(arpPacket.getSourceProtocolAddress()));
        } else {
            ipAddress = null;
        }
        final var addrs = createAddresses(ethernetPacket.getSourceMac(), vlanId, ipAddress,
            ethernetPacket.getEthertype());
        if (addrs != null) {
            hostTrackerImpl.packetReceived(addrs, rawPacket.getIngress().getValue());
        }
    }

    @NonNullByDefault
    private void onIpv4PacketReceived(final Ipv4PacketReceived packetReceived) {
        RawPacketFields rawPacket = null;
        EthernetPacket ethernetPacket = null;
        Ipv4Packet ipv4Packet = null;
        for (var packetChain : packetReceived.nonnullPacketChain()) {
            // TODO: use an enhanced switch when we have Java 21
            final var packet = packetChain.getPacket();
            if (packet instanceof RawPacket raw) {
                rawPacket = raw.getRawPacketFields();
            } else if (packet instanceof EthernetPacket ethernet) {
                ethernetPacket = ethernet;
            } else if (packet instanceof Ipv4Packet ipv4) {
                ipv4Packet = ipv4;
            }
        }
        if (rawPacket == null || ethernetPacket == null || ipv4Packet == null) {
            return;
        }

        if (IPV4_IP_TO_IGNORE.equals(ipv4Packet.getSourceIpv4().getValue())) {
            return;
        }

        VlanId vlanId = null;
        if (ethernetPacket.getEthertype().equals(KnownEtherType.VlanTagged)) {
            vlanId = ethernetPacket.getHeader8021q().get(0).getVlan();
        }
        MacAddress sourceMac = ethernetPacket.getSourceMac();
        IpAddress ipAddress = new IpAddress(ipv4Packet.getSourceIpv4());

        Addresses addrs = createAddresses(sourceMac, vlanId, ipAddress, ethernetPacket.getEthertype());
        if (addrs == null) {
            return;
        }
        NodeConnectorRef ingress = rawPacket.getIngress();
        hostTrackerImpl.packetReceived(addrs, ingress.getValue());
    }

    @NonNullByDefault
    private void onIpv6PacketReceived(final Ipv6PacketReceived packetReceived) {
        RawPacketFields rawPacket = null;
        EthernetPacket ethernetPacket = null;
        Ipv6Packet ipv6Packet = null;
        for (var packetChain : packetReceived.nonnullPacketChain()) {
            // TODO: use an enhanced switch when we have Java 21
            final var packet = packetChain.getPacket();
            if (packet instanceof RawPacket raw) {
                rawPacket = raw.getRawPacketFields();
            } else if (packet instanceof EthernetPacket ethernet) {
                ethernetPacket = ethernet;
            } else if (packet instanceof Ipv6Packet ipv6) {
                ipv6Packet = ipv6;
            }
        }
        if (rawPacket == null || ethernetPacket == null || ipv6Packet == null) {
            return;
        }

        if (IPV6_IP_TO_IGNORE.equals(ipv6Packet.getSourceIpv6().getValue())) {
            return;
        }

        final VlanId vlanId;
        if (ethernetPacket.getEthertype().equals(KnownEtherType.VlanTagged)) {
            vlanId = ethernetPacket.getHeader8021q().get(0).getVlan();
        } else {
            vlanId = null;
        }

        final var addrs = createAddresses(ethernetPacket.getSourceMac(), vlanId,
            new IpAddress(ipv6Packet.getSourceIpv6()), ethernetPacket.getEthertype());
        if (addrs != null) {
            hostTrackerImpl.packetReceived(addrs, rawPacket.getIngress().getValue());
        }
    }

    private static Addresses createAddresses(final MacAddress srcMacAddr, final VlanId vlanId,
            final IpAddress srcIpAddr, final KnownEtherType ketype) {
        AddressesBuilder addrs = new AddressesBuilder();
        if (srcMacAddr == null || srcIpAddr == null) {
            return null;
        }
        /*
         * TODO: if this is used, use a ReadWriteTranscation to figure out if
         * there is an already existing addresses that has the same MAC, IP,
         * VLAN triple and use it’s ID then, if there’s none, then we make up
         * our own Addresses
         */
        BigInteger id = BigInteger.valueOf(ketype.getIntValue()).abs()
                .add(BigInteger.valueOf(srcMacAddr.hashCode()).abs().shiftLeft(16));
        addrs.setId(Uint64.valueOf(id));
        addrs.withKey(new AddressesKey(addrs.getId()));
        addrs.setVlan(vlanId);
        addrs.setIp(srcIpAddr);
        addrs.setMac(srcMacAddr);
        // addrs.setFirstSeen(new Date().getTime());
        addrs.setLastSeen(new Date().getTime());
        return addrs.build();
    }
}
