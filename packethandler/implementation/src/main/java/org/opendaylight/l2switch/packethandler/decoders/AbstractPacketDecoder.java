package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.Packet;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * A base class for all decoders. Each extended decoder should also implement a notification listener
 * that it can consume. And make use of
 */
public abstract class AbstractPacketDecoder<N extends Notification> implements  NotificationProviderService.NotificationInterestListener{


  private Class<N> producedPacketNotificationType;
  private NotificationProviderService notificationProviderService;
  private int listenerCount=0;

  /**
   * Constructor to
   * @param producedPacketNotificationType
   * @param notificationProviderService
   */
  public  AbstractPacketDecoder(Class<N> producedPacketNotificationType, NotificationProviderService notificationProviderService) {
    this.producedPacketNotificationType = producedPacketNotificationType;
    this.notificationProviderService = notificationProviderService;
    notificationProviderService.registerInterestListener(this);
  }

  /**
   * Keeps track of listeners registered for the notification that a decoder produces.
   * @param aClass
   */
  @Override
  public void onNotificationSubscribtion(Class<? extends Notification> aClass) {
    if (aClass !=null && aClass.isAssignableFrom(producedPacketNotificationType)) {
      listenerCount++;
    }
  }

  /**
   * TODO: This method is not there today but planning to propose it in MD_SAL notification service,
   * TODO: as it would be useful to know un-subscriptions to a notification as well.
   *
   * Keeps track of listeners unregistered for the notification that a decoder produces.
   * @param aClass
  @Override
  public void onNotificationUnSubscription(Class<? extends Notification> aClass) {
    if (aClass !=null && aClass.isAssignableFrom(producedPacketNotificationType)) {
      listenerCount--;
    }
  }
   */


  /**
   * Every extended decoder should call this method on a receipt of a input packet notification.
   * This method would make sure it decodes only when necessary and publishes corresponding event
   * on successful decoding.
   * @param packet
   */
  public void decodeAndPublish(Packet packet) {
    Packet decodedPacket = null;
    if(listenerCount>0) {
      decodedPacket = decode(packet);
      if(decodedPacket!=null) {
        N packetNotification = buildPacketNotification(decodedPacket);
        if(packetNotification != null) {
          notificationProviderService.publish(packetNotification);
        }
      }
    }
  }
  /**
   * Decodes the payload in given Packet further and returns a extension of Packet.
   * e.g. ARP, IPV4, LLDP etc.
   *
   * @param packet
   * @return
   */
  public abstract Packet decode(Packet packet);

  /**
   * This is utility method for converting the decoded packet to its corresponding notification.
   *
   * @param decodedPacket
   * @return
   */
  public abstract N buildPacketNotification(Packet decodedPacket);
}
