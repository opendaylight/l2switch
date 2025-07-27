/*
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketChainGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6Packet;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.PropertyIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * A Simple Address Observer based on l2switch address observer.
 */
public class SimpleAddressObserver {
    @NonNullByDefault
    private record MatchedPacket<T extends Packet & DataObject>(RawPacket raw, EthernetPacket ethernet, T protocol) {
        MatchedPacket {
            requireNonNull(raw);
            requireNonNull(ethernet);
            requireNonNull(protocol);
        }

        static <T extends Packet & DataObject> @Nullable MatchedPacket<T> find(final Class<T> protoClass,
                final PacketChainGrp packetChainHolder) {
            RawPacket rawPacket = null;
            EthernetPacket ethernetPacket = null;
            @Nullable T protoPacket = null;
            for (var packetChain : packetChainHolder.nonnullPacketChain()) {
                // TODO: use an enhanced switch when we have Java 21
                final var packet = packetChain.getPacket();
                if (packet instanceof RawPacket raw) {
                    rawPacket = raw;
                } else if (packet instanceof EthernetPacket ethernet) {
                    ethernetPacket = ethernet;
                } else if (protoClass.isInstance(packet)) {
                    protoPacket = protoClass.cast(packet);
                }
            }

            return rawPacket == null || ethernetPacket == null || protoPacket == null ? null
                : new MatchedPacket<>(rawPacket, ethernetPacket, protoPacket);
        }

        @Nullable Addresses createAddresses(final IpAddress srcIpAddr) {
            final var srcMacAddr = ethernet.getSourceMac();
            if (srcMacAddr == null) {
                return null;
            }

            /*
             * TODO: if this is used, use a ReadWriteTranscation to figure out if there is an already existing addresses
             *       that has the same MAC, IP, VLAN triple and use it’s ID then, if there’s none, then we make up
             *       our own Addresses
             */
            // FIXME: use long math for this
            final var id = BigInteger.valueOf(ethernet.getEthertype().getIntValue()).abs()
                .add(BigInteger.valueOf(srcMacAddr.hashCode()).abs().shiftLeft(16));

            return new AddressesBuilder()
                .setId(Uint64.valueOf(id))
                .setVlan(ethernet.getEthertype() == KnownEtherType.VlanTagged
                    // TODO: getFirst() when we have Java 21
                    ? ethernet.nonnullHeader8021q().get(0).getVlan() : null)
                .setIp(requireNonNull(srcIpAddr))
                .setMac(srcMacAddr)
                // addrs.setFirstSeen(new Date().getTime())
                .setLastSeen(Instant.now().toEpochMilli())
                .build();
        }

        InstanceIdentifier<?> ingress() {
            final var id = raw.getRawPacketFields().getIngress().getValue();
            return switch (id) {
                case DataObjectIdentifier<?> doi -> doi.toLegacy();
                case PropertyIdentifier<?, ?> pi -> pi.container().toLegacy();
            };
        }
    }

    private static final Ipv4Address IPV4_IP_TO_IGNORE = new Ipv4Address("0.0.0.0");
    private static final Ipv6Address IPV6_IP_TO_IGNORE = new Ipv6Address("0:0:0:0:0:0:0:0");

    private final HostTrackerImpl hostTrackerImpl;
    private final NotificationService notificationService;

    public SimpleAddressObserver(final HostTrackerImpl hostTrackerImpl, final NotificationService notificationService) {
        this.hostTrackerImpl = requireNonNull(hostTrackerImpl);
        this.notificationService = requireNonNull(notificationService);
    }

    Registration registerAsNotificationListener() {
        return notificationService.registerCompositeListener(new CompositeListener(Set.of(
            new CompositeListener.Component<>(ArpPacketReceived.class, this::onArpPacketReceived),
            new CompositeListener.Component<>(Ipv4PacketReceived.class, this::onIpv4PacketReceived),
            new CompositeListener.Component<>(Ipv6PacketReceived.class, this::onIpv6PacketReceived))));
    }

    @NonNullByDefault
    private void onArpPacketReceived(final ArpPacketReceived received) {
        final var matched = MatchedPacket.find(ArpPacket.class, received);
        if (matched == null) {
            return;
        }

        final var protocol = matched.protocol();
        if (protocol.getProtocolType() != KnownEtherType.Ipv4) {
            return;
        }

        final var addrs = matched.createAddresses(
            new IpAddress(new Ipv4Address(protocol.getSourceProtocolAddress())));
        if (addrs != null) {
            hostTrackerImpl.packetReceived(addrs, matched.ingress());
        }
    }

    @NonNullByDefault
    private void onIpv4PacketReceived(final Ipv4PacketReceived received) {
        final var matched = MatchedPacket.find(Ipv4Packet.class, received);
        if (matched == null) {
            return;
        }

        final var sourceIp = matched.protocol().getSourceIpv4();
        if (IPV4_IP_TO_IGNORE.equals(sourceIp)) {
            return;
        }

        final var addrs = matched.createAddresses(new IpAddress(sourceIp));
        if (addrs != null) {
            hostTrackerImpl.packetReceived(addrs, matched.ingress());
        }
    }

    @NonNullByDefault
    private void onIpv6PacketReceived(final Ipv6PacketReceived received) {
        final var matched = MatchedPacket.find(Ipv6Packet.class, received);
        if (matched == null) {
            return;
        }

        final var sourceIp = matched.protocol().getSourceIpv6();
        if (IPV6_IP_TO_IGNORE.equals(sourceIp)) {
            return;
        }

        final var addrs = matched.createAddresses(new IpAddress(sourceIp));
        if (addrs != null) {
            hostTrackerImpl.packetReceived(addrs, matched.ingress());
        }
    }
}
