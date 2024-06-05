/*
 * Copyright (c) 2016 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.address.tracker.config.rev160621.AddressTrackerConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressTrackerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AddressTrackerProvider.class);
    private static final String ARP_PACKET_TYPE = "arp";
    private static final String IPV4_PACKET_TYPE = "ipv4";
    private static final String IPV6_PACKET_TYPE = "ipv6";

    private final List<Registration> listenerRegistrations = new ArrayList<>();
    private final NotificationService notificationService;
    private final DataBroker dataBroker;
    private final long timestampUpdateInterval;
    private final String observerAddressesFrom;

    public AddressTrackerProvider(final DataBroker dataBroker, final NotificationService notificationService,
            final AddressTrackerConfig config) {
        this.notificationService = notificationService;
        this.dataBroker = dataBroker;
        this.timestampUpdateInterval = config.getTimestampUpdateInterval().longValue();
        this.observerAddressesFrom = config.getObserveAddressesFrom();
    }

    public void init() {
        // Setup AddressObserver & AddressObservationWriter
        AddressObservationWriter addressObservationWriter = new AddressObservationWriter(dataBroker);
        addressObservationWriter.setTimestampUpdateInterval(timestampUpdateInterval);
        Set<String> packetTypes = processObserveAddressesFrom(observerAddressesFrom);

        if (packetTypes.isEmpty()) { // set default to arp
            packetTypes = new HashSet<>();
            packetTypes.add(ARP_PACKET_TYPE);
        }

        if (packetTypes.contains(ARP_PACKET_TYPE)) {
            // Register AddressObserver for notifications
            this.listenerRegistrations.add(notificationService.registerListener(ArpPacketReceived.class,
                new AddressObserverUsingArp(addressObservationWriter)));
        }

        if (packetTypes.contains(IPV4_PACKET_TYPE)) {
            // Register AddressObserver for notifications
            this.listenerRegistrations.add(notificationService.registerListener(Ipv4PacketReceived.class,
                new AddressObserverUsingIpv4(addressObservationWriter)));
        }
        if (packetTypes.contains(IPV6_PACKET_TYPE)) {
            // Register AddressObserver for notifications
            this.listenerRegistrations.add(notificationService.registerListener(Ipv6PacketReceived.class,
                new AddressObserverUsingIpv6(addressObservationWriter)));
        }
        LOG.info("AddressTracker initialized.");
    }

    public void close() {
        listenerRegistrations.forEach(reg -> reg.close());
        LOG.info("AddressTracker torn down.");
    }

    private static @NonNull Set<String> processObserveAddressesFrom(String observeAddressesFrom) {
        Set<String> packetTypes = new HashSet<>();
        if (observeAddressesFrom == null || observeAddressesFrom.isEmpty()) {
            packetTypes.add(ARP_PACKET_TYPE);
            return packetTypes;
        }
        String[] observeAddressFromSplit = observeAddressesFrom.split(",");
        if (observeAddressFromSplit.length == 0) {
            packetTypes.add(ARP_PACKET_TYPE);
            return packetTypes;
        }
        for (String packetType : observeAddressFromSplit) {
            packetTypes.add(packetType);
        }
        return packetTypes;
    }
}
