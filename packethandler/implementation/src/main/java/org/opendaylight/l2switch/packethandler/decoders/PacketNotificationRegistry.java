package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * PacketNotificationRegistry maintains mapping of NotificationType to EtherType. It also maintains
 * the count of Listener that are subscribed to a particular notification. This way it can specify if there
 * is any active listener subscription for any particular EtherType notification.
 */
public class PacketNotificationRegistry implements NotificationProviderService.NotificationInterestListener {
  /**
   * Increments the listener count for given notification type.
   *
   * @param aClass
   */
  @Override
  public void onNotificationSubscribtion(Class<? extends Notification> aClass) {

  }

  /**
   * Maintains map of EtherType to notificationType
   *
   * @param notificationType
   * @param <C>
   */
  public <C extends Notification> void trackPacketNotification(KnownEtherType etherType, Class<C> notificationType) {

  }

  /**
   * Checks if a listener is subscribed to notification that is associated with given EtherType.
   *
   * @param etherType
   * @return
   */
  public boolean isListenerSubscribed(KnownEtherType etherType) {
    return false;
  }
}
