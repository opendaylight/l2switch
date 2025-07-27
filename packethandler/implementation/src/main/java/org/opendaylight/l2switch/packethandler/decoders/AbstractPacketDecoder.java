/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.NotificationService.Listener;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * A base class for all decoders. Each extended decoder should also implement a notification listener that it can
 * consume.
 */
// FIXME: Implement NotificationInterestLister equivalent when it is available again
public abstract class AbstractPacketDecoder<C, P extends Notification<?>> implements AutoCloseable {

    //private final Class<P> producedPacketNotificationType;
    private final NotificationPublishService notificationProviderService;
    private final NotificationService notificationService;

    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService decodeAndPublishExecutor = Executors.newFixedThreadPool(CPUS);

    protected Registration listenerRegistration;

    /**
     * Constructor.
     */
    @SuppressFBWarnings("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR")
    public AbstractPacketDecoder(Class<P> producedPacketNotificationType,
                                 NotificationPublishService notificationProviderService,
                                 NotificationService notificationService) {
        // this.producedPacketNotificationType = producedPacketNotificationType;
        this.notificationProviderService = notificationProviderService;
        this.notificationService = notificationService;
        listenerRegistration = this.notificationService.registerListener(getPacketType(), getConsumedListener());
    }

//    /**
//     * Keeps track of listeners registered for the notification that a decoder
//     * produces.
//     */
//    public synchronized void onNotificationSubscribtion(Class<? extends Notification> clazz) {
//        if (clazz != null && clazz.equals(producedPacketNotificationType)) {
//            if (listenerRegistration == null) {
//                NotificationListener notificationListener = getConsumedNotificationListener();
//                listenerRegistration = notificationProviderService.registerNotificationListener(notificationListener);
//            }
//        }
//    }

    /**
     * Every extended decoder should call this method on a receipt of a input
     * packet notification. This method would make sure it decodes only when
     * necessary and publishes corresponding event on successful decoding.
     */
    public void decodeAndPublish(final C consumedPacketNotification) {
        decodeAndPublishExecutor.execute(() -> {
            P packetNotification = null;
            if (consumedPacketNotification != null && canDecode(consumedPacketNotification)) {
                packetNotification = decode(consumedPacketNotification);
            }
            if (packetNotification != null) {
                try {
                    notificationProviderService.putNotification(packetNotification);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while publishing notification", e);
                }
            }
        });
    }

    /**
     * Decodes the payload in given Packet further and returns a extension of
     * Packet. e.g. ARP, IPV4, LLDP etc.
     */
    public abstract P decode(C consumedPacketNotification);

    public abstract Listener<?> getConsumedListener();

    public abstract Class getPacketType();

    public abstract boolean canDecode(C consumedPacketNotification);

    @Override
    public void close() {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
        decodeAndPublishExecutor.shutdown();
    }
}
