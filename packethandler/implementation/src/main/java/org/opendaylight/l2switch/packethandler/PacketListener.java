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

// FIXME: implement NotificationPublishService.DemandMonitor using commented out code
final class PacketListener<
        C extends Notification<C> & DataObject,
        P extends Notification<P> & DataObject & PacketChainGrp & PacketPayload>
        implements NotificationService.Listener<C> {
    private final @NonNull NotificationPublishService notificationPublishService;
    private final @NonNull PacketDecoder<C, P> decoder;
    private final @NonNull ExecutorService executor;

    private Registration consumerReg;

    PacketListener(final NotificationPublishService notificationPublishService,
            final NotificationService notificationService, final PacketDecoder<C, P> decoder) {
        this.notificationPublishService = requireNonNull(notificationPublishService);
        requireNonNull(notificationService);
        this.decoder = requireNonNull(decoder);

        // FIXME: use a better/shared executor
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        consumerReg = notificationService.registerListener(decoder.consumedType(), this, executor);
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
        // FIXME: improve calling convention: canDecode() and decode() should be combined into tryDecode()
        if (!decoder.canDecode(notification)) {
            return;
        }

        final var decoded = decoder.decode(notification);
        if (decoded == null) {
            return;
        }

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
