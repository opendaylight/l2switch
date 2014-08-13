/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

/**
 * A base class for all decoders. Each extended decoder should also implement a notification listener
 * that it can consume. And make use of
 */
public abstract class AbstractPacketDecoder<ConsumedPacketNotification, ProducedPacketNotification extends Notification>
    implements  NotificationProviderService.NotificationInterestListener , AutoCloseable {


  private Class<ProducedPacketNotification> producedPacketNotificationType;
  private NotificationProviderService notificationProviderService;


  protected Registration listenerRegistration;

  /**
   * Constructor to
   * @param producedPacketNotificationType
   * @param notificationProviderService
   */
  public  AbstractPacketDecoder(Class<ProducedPacketNotification> producedPacketNotificationType, NotificationProviderService notificationProviderService) {
    this.producedPacketNotificationType = producedPacketNotificationType;
    this.notificationProviderService = notificationProviderService;
    notificationProviderService.registerInterestListener(this);
  }


  /**
   * Keeps track of listeners registered for the notification that a decoder produces.
   * @param aClass
   */
  @Override
  public synchronized void onNotificationSubscribtion(Class<? extends Notification> aClass) {
    if (aClass !=null && aClass.equals(producedPacketNotificationType)) {
      if(listenerRegistration == null) {
        NotificationListener notificationListener = getConsumedNotificationListener();
        listenerRegistration = notificationProviderService.registerNotificationListener(notificationListener);
      }
    }
  }


  /**
   * Every extended decoder should call this method on a receipt of a input packet notification.
   * This method would make sure it decodes only when necessary and publishes corresponding event
   * on successful decoding.
   */
  public void decodeAndPublish(ConsumedPacketNotification consumedPacketNotification) {
    ProducedPacketNotification packetNotification=null;
    if(consumedPacketNotification!= null && canDecode(consumedPacketNotification)) {
      packetNotification = decode(consumedPacketNotification);
    }
    if(packetNotification != null) {
      notificationProviderService.publish(packetNotification);
    }
  }
  /**
   * Decodes the payload in given Packet further and returns a extension of Packet.
   * e.g. ARP, IPV4, LLDP etc.
   *
   * @return
   */
  public abstract ProducedPacketNotification decode(ConsumedPacketNotification consumedPacketNotification);


  public abstract NotificationListener getConsumedNotificationListener();

  public abstract boolean canDecode(ConsumedPacketNotification consumedPacketNotification);


  @Override
  public void close() throws Exception {
    if(listenerRegistration != null) {
      listenerRegistration.close();
    }
  }
}
