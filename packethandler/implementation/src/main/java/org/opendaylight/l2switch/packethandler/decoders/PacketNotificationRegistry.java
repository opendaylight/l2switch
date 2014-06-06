package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yangtools.yang.binding.Notification;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * PacketNotificationRegistry maintains mapping of NotificationType to EtherType. It also maintains
 * the count of Listener that are subscribed to a particular notification. This way it can specify if there
 * is any active listener subscription for any particular EtherType notification.
 */
public class PacketNotificationRegistry implements NotificationProviderService.NotificationInterestListener {
  private Map<KnownEtherType, Class<? extends Notification>> etherTypeToPacketNotificationTypeMap = new EnumMap<KnownEtherType, Class<? extends Notification>>(KnownEtherType.class);

  private Map<Class<? extends Notification>, Integer> packetNotificationTypeToListenerCountMap = new HashMap<Class<? extends Notification>, Integer>();
  /**
   * Increments the listener count for given notification type.
   *
   * @param aClass
   */
  @Override
  public void onNotificationSubscribtion(Class<? extends Notification> aClass) {
    if(aClass == null) return;

    int listenerCount = packetNotificationTypeToListenerCountMap.get(aClass);
    packetNotificationTypeToListenerCountMap.put(aClass,listenerCount++);

  }

  /**
   * Maintains map of EtherType to notificationType
   *
   * @param notificationType
   * @param <C>
   */
  public <C extends Notification> void trackPacketNotificationListener(KnownEtherType etherType, Class<C> notificationType) {
    if(etherType == null || notificationType == null) return;

    synchronized(this){
      etherTypeToPacketNotificationTypeMap.put(etherType,notificationType);
    }

  }

  /**
   * Checks if a listener is subscribed to notification that is associated with given EtherType.
   *
   * @param etherType
   * @return
   */
  public boolean isListenerSubscribed(KnownEtherType etherType) {
    if(etherType == null) return false;

    int listenerCount = packetNotificationTypeToListenerCountMap.get(etherTypeToPacketNotificationTypeMap.get(etherType));

    return listenerCount>0;
  }
}
