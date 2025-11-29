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
import java.util.concurrent.atomic.AtomicLong;
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
    private static final AtomicLong POOL_COUNTER = new AtomicLong();

    private final @NonNull NotificationPublishService notificationPublishService;
    private final @NonNull AbstractDecoder<C, P> decoder;
    private final @NonNull ExecutorService executor;

    private Registration consumerReg;

    PacketListener(final NotificationPublishService notificationPublishService,
            final NotificationService notificationService, final AbstractDecoder<C, P> decoder) {
        this.notificationPublishService = requireNonNull(notificationPublishService);
        requireNonNull(notificationService);
        this.decoder = requireNonNull(decoder);

        // FIXME: This is not nice: we are creating a fixed-size thread pool with normal priority competing for all
        //        available cores and we do that for every decoder -- i.e. currently we are spawning 5xCORES threads.
        //        These threads serve a dual purpose:
        //          - they offload packet processing from the NotificationService pool, which is what we need to do
        //            based on the contract of that service
        //          - they allow for blocking (see onNotification() below), which is probably not what we want
        //
        //        This is also a side-effect of us wiring FirstDecoder and downstream SubsequentDecoder(s) via
        //        Notification(Publish)Service. That in and of itself is causing unnecessary thread ping-pongs, for
        //        example in the case of IPv4 ICMP:
        //          1. we receive PacketReceived on this thread EthernetDecoder thread pool
        //          2. we decode the packet and push it into NotificationPublishService (which is a different thread)
        //          3. we receive the notification on Ipv4Decoder thread pool
        //          4. we decode the packet and push it into NotificationPublishService again
        //          5. we receive the notification on IcmpDecoder thread pool
        //          6. we decode the packet and push it into NotificationPublishService again
        //        So we switch processing threads 6 times to decode a single packet, all of which are competing for
        //        CPU cores, potentially blocking (3 times) on NotificationPublishService and causing potential wakeups
        //        (3 times).
        //
        //        At the end of the day we should probably have lower-priority CORES threads which are fed by
        //        NotificationService delivering FirstDecoder inputs and those threads process the entire pipeline
        //        producing only the notifications DemandListeners indicate. Event publishing should be non-blocking
        //        (i.e. dropping output when NotificationPublishService is clogged).
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
            Thread.ofPlatform()
                // i.e. "l2switch-packethandler-0-EthernetDecoder-1"
                .name("l2switch-packethandler-" + POOL_COUNTER.getAndIncrement() + "-"
                    + decoder.getClass().getSimpleName() + "-", 0)
                // uncaught exceptions are a cause for warning, we just end up losing a packet
                .uncaughtExceptionHandler((thread, cause) -> LOG.warn("Thread {} failed unexpectedly", thread, cause))
                // okay to terminate JVM
                .daemon()
                .factory());

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
        // FIXME: improve calling convention: canDecode() and decode() should be combined into tryDecode()
        if (!decoder.canDecode(notification)) {
            return;
        }

        final var decoded = decoder.decode(notification);
        if (decoded == null) {
            return;
        }

        // FIXME: This call will block current thread if the queues are clogged up. At the end of the day, since we are
        //        an L2 switch, we should be able to to just drop the packet.
        //        This is quite related to the executor pool above and what it is meant to achieve.
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

            final var nrTasks = executor.shutdownNow().size();
            LOG.debug("Executor for {} shut down with {} pending tasks", decoder.getClass().getSimpleName(), nrTasks);
        }
    }
}
