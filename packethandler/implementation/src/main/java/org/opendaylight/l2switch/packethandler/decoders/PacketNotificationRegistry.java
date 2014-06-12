package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packethandler.packet.rev140528.packet.PacketPayloadType;
import org.opendaylight.yangtools.yang.binding.Notification;

import java.util.HashMap;
import java.util.Map;

/**
 * PacketNotificationRegistry maintains mapping of NotificationType to EtherType. It also maintains
 * the count of Listener that are subscribed to a particular notification. This way it can specify if there
 * is any active listener subscription for any particular EtherType notification.
 */
public class PacketNotificationRegistry implements NotificationProviderService.NotificationInterestListener {
  private Map<PacketPayloadType, Class<? extends Notification>> etherTypeToPacketNotificationTypeMap = new HashMap<PacketPayloadType, Class<? extends Notification>>();

  private Map<Class<? extends Notification>, Integer> packetNotificationTypeToListenerCountMap = new HashMap<Class<? extends Notification>, Integer>();

  /**
   * Increments the listener count for given notification type.
   *
   * @param aClass
   */
  @Override
  public void onNotificationSubscribtion(Class<? extends Notification> aClass) {
    if(aClass == null) return;

    synchronized(this) {
      Integer listenerCount = packetNotificationTypeToListenerCountMap.get(aClass);
      if(listenerCount == null)
        listenerCount = 0;
      packetNotificationTypeToListenerCountMap.put(aClass, ++listenerCount);
    }
  }

  /**
   * Maintains map of EtherType to notificationType
   *
   * @param notificationType
   * @param <N>
   */
  public <N extends Notification> void trackPacketNotificationListener(PacketPayloadType packetPayloadType, Class<N> notificationType) {
    if(packetPayloadType == null || notificationType == null) return;

    synchronized(this) {
      etherTypeToPacketNotificationTypeMap.put(packetPayloadType, notificationType);
    }

  }

  /**
   * Checks if a listener is subscribed to notification that is associated with given EtherType.
   *
   * @param packetPayloadType
   * @return
   */
  public boolean isListenerSubscribed(PacketPayloadType packetPayloadType) {
    if(packetPayloadType == null) return false;

    Class<?> packetNotification = etherTypeToPacketNotificationTypeMap.get(packetPayloadType);
    if(packetNotification == null) return false;

    return isListenerSubscribed((Class<? extends Notification>) packetNotification);
  }

  /**
   * Checks if a listener is subscribed to give the notification type .
   *
   * @param packetNotificationType
   * @return
   */
  public boolean isListenerSubscribed(Class<? extends Notification> packetNotificationType) {
    if(packetNotificationType == null) return false;

    Integer listenerCount = packetNotificationTypeToListenerCountMap.get(packetNotificationType);

    if(listenerCount == null) return false;

    return listenerCount > 0;
  }
}
