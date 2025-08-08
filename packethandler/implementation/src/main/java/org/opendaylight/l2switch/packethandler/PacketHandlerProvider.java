/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import com.google.common.collect.ImmutableSet;
import org.opendaylight.l2switch.packethandler.decoders.AbstractPacketDecoder;
import org.opendaylight.l2switch.packethandler.decoders.ArpDecoder;
import org.opendaylight.l2switch.packethandler.decoders.EthernetDecoder;
import org.opendaylight.l2switch.packethandler.decoders.IcmpDecoder;
import org.opendaylight.l2switch.packethandler.decoders.Ipv4Decoder;
import org.opendaylight.l2switch.packethandler.decoders.Ipv6Decoder;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHandlerProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PacketHandlerProvider.class);

    private final ImmutableSet<AbstractPacketDecoder<?, ?>> decoders;

    public PacketHandlerProvider(final NotificationPublishService notificationPublishService,
            final NotificationService notificationService) {
        decoders = new ImmutableSet.Builder<AbstractPacketDecoder<?, ?>>()
            .add(new EthernetDecoder(notificationPublishService, notificationService))
            .add(new ArpDecoder(notificationPublishService, notificationService))
            .add(new Ipv4Decoder(notificationPublishService, notificationService))
            .add(new Ipv6Decoder(notificationPublishService, notificationService))
            .add(new IcmpDecoder(notificationPublishService, notificationService))
            .build();
        LOG.info("PacketHandler initialized.");
    }

    @Override
    public void close() {
        for (AbstractPacketDecoder<?, ?> decoder : decoders) {
            decoder.close();
        }
        LOG.info("PacketHandler (instance {}) torn down.", this);
    }
}
