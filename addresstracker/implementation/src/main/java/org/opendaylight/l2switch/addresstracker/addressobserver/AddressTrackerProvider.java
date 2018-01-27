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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.address.tracker.config.rev160621.AddressTrackerConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressTrackerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AddressTrackerProvider.class);
    private static final String ARP_PACKET_TYPE = "arp";
    private static final String IPV4_PACKET_TYPE = "ipv4";
    private static final String IPV6_PACKET_TYPE = "ipv6";

    private final List<Registration> listenerRegistrations = new ArrayList<>();
    private final NotificationProviderService notificationService;
    private final DataBroker dataBroker;
    private final Long timestampUpdateInterval;
    private final String observerAddressesFrom;

    public AddressTrackerProvider(final DataBroker dataBroker,
            final NotificationProviderService notificationProviderService,
            final AddressTrackerConfig config) {
        this.notificationService = notificationProviderService;
        this.dataBroker = dataBroker;
        this.timestampUpdateInterval = config.getTimestampUpdateInterval();
        this.observerAddressesFrom = config.getObserveAddressesFrom();
    }

    public void init() {
        // Setup AddressObserver & AddressObservationWriter
        AddressObservationWriter addressObservationWriter = new AddressObservationWriter(dataBroker);
        addressObservationWriter.setTimestampUpdateInterval(timestampUpdateInterval);
        Set<String> packetTypes = processObserveAddressesFrom(observerAddressesFrom);

        if (packetTypes == null || packetTypes.isEmpty()) { // set default to
                                                            // arp
            packetTypes = new HashSet<>();
            packetTypes.add(ARP_PACKET_TYPE);
        }

        if (packetTypes.contains(ARP_PACKET_TYPE)) {
            AddressObserverUsingArp addressObserverUsingArp = new AddressObserverUsingArp(addressObservationWriter);
            // Register AddressObserver for notifications
            this.listenerRegistrations.add(notificationService.registerNotificationListener(addressObserverUsingArp));
        }

        if (packetTypes.contains(IPV4_PACKET_TYPE)) {
            AddressObserverUsingIpv4 addressObserverUsingIpv4 = new AddressObserverUsingIpv4(addressObservationWriter);
            // Register AddressObserver for notifications
            this.listenerRegistrations.add(notificationService.registerNotificationListener(addressObserverUsingIpv4));
        }
        if (packetTypes.contains(IPV6_PACKET_TYPE)) {
            AddressObserverUsingIpv6 addressObserverUsingIpv6 = new AddressObserverUsingIpv6(addressObservationWriter);
            // Register AddressObserver for notifications
            this.listenerRegistrations.add(notificationService.registerNotificationListener(addressObserverUsingIpv6));
        }

        LOG.info("AddressTracker initialized.");
    }

    public void close() {
        listenerRegistrations.forEach(reg -> reg.close());
        LOG.info("AddressTracker torn down.", this);
    }

    private Set<String> processObserveAddressesFrom(String observeAddressesFrom) {
        Set<String> packetTypes = new HashSet<>();
        if (observeAddressesFrom == null || observeAddressesFrom.isEmpty()) {
            packetTypes.add(ARP_PACKET_TYPE);
            return packetTypes;
        }
        String[] observeAddressFromSplit = observeAddressesFrom.split(",");
        if (observeAddressFromSplit == null || observeAddressFromSplit.length == 0) {
            packetTypes.add(ARP_PACKET_TYPE);
            return packetTypes;
        }
        for (String packetType : observeAddressFromSplit) {
            packetTypes.add(packetType);
        }
        return packetTypes;
    }
}
