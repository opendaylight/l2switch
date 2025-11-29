/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketChainGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketPayload;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Glue between an {@link AbstractDecoder} and MD-SAL services.
 *
 * @param <C> consumed notification type
 * @param <P> produced notification type
 */
// FIXME: implement NotificationPublishService.DemandMonitor using commented out code
final class PacketListener<
        C extends Notification<C> & DataObject,
        P extends Notification<P> & DataObject & PacketChainGrp & PacketPayload>
        implements NotificationService.Listener<C> {
    private static final Logger LOG = LoggerFactory.getLogger(PacketListener.class);

    private final @NonNull NotificationPublishService notificationPublishService;
    private final @NonNull AbstractDecoder<C, P> decoder;
    private final @NonNull ExecutorService executor;

    private Registration consumerReg;

    PacketListener(final NotificationPublishService notificationPublishService,
            final NotificationService notificationService, final AbstractDecoder<C, P> decoder) {
        this.notificationPublishService = requireNonNull(notificationPublishService);
        requireNonNull(notificationService);
        this.decoder = requireNonNull(decoder);

        // FIXME: use a better/shared executor
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        consumerReg = notificationService.registerListener(decoder.consumedType(), this, executor);
        LOG.debug("Started processing {}", decoder);
    }

//  /**
//   * Keeps track of listeners registered for the notification that a decoder
//   * produces.
//   */
//  public synchronized void onNotificationSubscribtion(Class<? extends Notification> clazz) {
//      if (clazz != null && clazz.equals(producedPacketNotificationType)) {
//          if (listenerRegistration == null) {
//              NotificationListener notificationListener = getConsumedNotificationListener();
//              listenerRegistration = notificationProviderService.registerNotificationListener(notificationListener);
//          }
//      }
//  }

    @Override
    public void onNotification(final C notification) {
        final var decoded = decoder.tryDecode(notification);
        if (decoded == null) {
            LOG.debug("{} could not decode {}", decoder, notification);
            return;
        }

        // FIXME: We really want to publish notifications only if there are outside observers of the specific result.
        //        In case this is only an intermediate step towards an observer (via a downstream SubsequentDecoder), we
        //        want to bypass publishing here and continue decoding the packet on this thread.
        try {
            notificationPublishService.putNotification(decoded);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing notification", e);
        }
    }

    void close() {
        if (consumerReg != null) {
            consumerReg.close();
            consumerReg = null;
        }
        executor.shutdown();
    }
}
