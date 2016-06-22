/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import com.google.common.collect.ImmutableSet;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.packethandler.decoders.AbstractPacketDecoder;
import org.opendaylight.l2switch.packethandler.decoders.ArpDecoder;
import org.opendaylight.l2switch.packethandler.decoders.EthernetDecoder;
import org.opendaylight.l2switch.packethandler.decoders.Ipv4Decoder;
import org.opendaylight.l2switch.packethandler.decoders.Ipv6Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHandlerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PacketHandlerProvider.class);
    ImmutableSet<AbstractPacketDecoder> decoders;

    private final NotificationProviderService notificationService;

    public PacketHandlerProvider(final NotificationProviderService notificationService) {
        this.notificationService = notificationService;
    }

    public void initiateDecoders() {
        decoders = new ImmutableSet.Builder<AbstractPacketDecoder>()
                .add(new EthernetDecoder(notificationService)).add(new ArpDecoder(notificationService))
                .add(new Ipv4Decoder(notificationService)).add(new Ipv6Decoder(notificationService))
                .build();
        LOG.info("PacketHandler initialized.");
    }

    public void closeDecoders() throws Exception {
        if (decoders != null && !decoders.isEmpty()) {
            for (AbstractPacketDecoder decoder : decoders) {
                decoder.close();
            }
        }
        LOG.info("PacketHandler (instance {}) torn down.", this);
    }
}
