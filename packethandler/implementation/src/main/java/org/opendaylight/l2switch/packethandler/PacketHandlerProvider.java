/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import java.util.List;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.l2switch.packethandler.decoders.AbstractPacketDecoder;
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

    private final List<AbstractPacketDecoder<?, ?>> decoders;

    @Inject
    @Activate
    public PacketHandlerProvider(@Reference final NotificationPublishService notificationPublishService,
            final @Reference NotificationService notificationService) {
        decoders = List.of(
            new EthernetDecoder(notificationPublishService, notificationService),
            new ArpDecoder(notificationPublishService, notificationService),
            new Ipv4Decoder(notificationPublishService, notificationService),
            new Ipv6Decoder(notificationPublishService, notificationService),
            new IcmpDecoder(notificationPublishService, notificationService));
        LOG.info("PacketHandler initialized.");
    }

    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        decoders.forEach(AbstractPacketDecoder::close);
        LOG.info("PacketHandler (instance {}) torn down.", this);
    }
}
