/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.l2switch.packethandler.decoders.ArpDecoder;
import org.opendaylight.l2switch.packethandler.decoders.EthernetDecoder;
import org.opendaylight.l2switch.packethandler.decoders.IcmpDecoder;
import org.opendaylight.l2switch.packethandler.decoders.Ipv4Decoder;
import org.opendaylight.l2switch.packethandler.decoders.Ipv6Decoder;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = { })
public final class PacketHandlerProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PacketHandlerProvider.class);

    private final List<PacketListener<?, ?>> listeners;

    @Inject
    @Activate
    public PacketHandlerProvider(@Reference final NotificationPublishService notificationPublishService,
            final @Reference NotificationService notificationService) {
        // FIXME: do not hard-code decoders:
        //        - for @Inject discover them via ServiceLoader
        //        - for @Activate inject them via a greedy reference
        //        For that we need to sort the decoders by dependencies as well, to mirror below structure

        // Naming/layering things a bit messy here because of OSI/Internet layer mapping oddities.
        listeners = Stream.<AbstractDecoder<?, ?>>of(
            // L2: Data link
            new EthernetDecoder(),
            // L3: Network, but only Link layer
            new ArpDecoder(),
            // L3: Network, but only Internet layer
            new Ipv4Decoder(), new Ipv6Decoder(),
            // L4: Transport, but also higher-level protocols from Internet layer
            new IcmpDecoder())
            .map(decoder -> new PacketListener<>(notificationPublishService, notificationService, decoder))
            .collect(Collectors.toUnmodifiableList());

        LOG.info("PacketHandler initialized.");
    }

    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        listeners.forEach(PacketListener::close);
        LOG.info("PacketHandler (instance {}) torn down.", this);
    }
}
