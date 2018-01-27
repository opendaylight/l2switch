/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

/**
 * A base class for all decoders. Each extended decoder should also implement a
 * notification listener that it can consume.
 */
public abstract class AbstractPacketDecoder<C, P extends Notification>
        implements NotificationProviderService.NotificationInterestListener, AutoCloseable {

    private final Class<P> producedPacketNotificationType;
    private final NotificationProviderService notificationProviderService;

    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService decodeAndPublishExecutor = Executors.newFixedThreadPool(CPUS);

    protected Registration listenerRegistration;

    /**
     * Constructor.
     */
    public AbstractPacketDecoder(Class<P> producedPacketNotificationType,
            NotificationProviderService notificationProviderService) {
        this.producedPacketNotificationType = producedPacketNotificationType;
        this.notificationProviderService = notificationProviderService;
        notificationProviderService.registerInterestListener(this);
    }

    /**
     * Keeps track of listeners registered for the notification that a decoder
     * produces.
     */
    @Override
    public synchronized void onNotificationSubscribtion(Class<? extends Notification> clazz) {
        if (clazz != null && clazz.equals(producedPacketNotificationType)) {
            if (listenerRegistration == null) {
                NotificationListener notificationListener = getConsumedNotificationListener();
                listenerRegistration = notificationProviderService.registerNotificationListener(notificationListener);
            }
        }
    }

    /**
     * Every extended decoder should call this method on a receipt of a input
     * packet notification. This method would make sure it decodes only when
     * necessary and publishes corresponding event on successful decoding.
     */
    public void decodeAndPublish(final C consumedPacketNotification) {
        decodeAndPublishExecutor.submit(() -> {
            P packetNotification = null;
            if (consumedPacketNotification != null && canDecode(consumedPacketNotification)) {
                packetNotification = decode(consumedPacketNotification);
            }
            if (packetNotification != null) {
                notificationProviderService.publish(packetNotification);
            }
        });
    }

    /**
     * Decodes the payload in given Packet further and returns a extension of
     * Packet. e.g. ARP, IPV4, LLDP etc.
     *
     * @return
     */
    public abstract P decode(C consumedPacketNotification);

    public abstract NotificationListener getConsumedNotificationListener();

    public abstract boolean canDecode(C consumedPacketNotification);

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
        decodeAndPublishExecutor.shutdown();
    }
}
