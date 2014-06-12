package org.opendaylight.l2switch.packethandler.decoders;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

import static junit.framework.Assert.assertEquals;


/**
 * Created by amitmandke on 6/5/14.
 */
public class PacketNotificationRegistryTest {
  PacketNotificationRegistry packetNotificationRegistry = null;
  private PacketPayloadType packetPayloadType;

  @Before
  public void init() {
    packetNotificationRegistry = new PacketNotificationRegistry();
    packetPayloadType = new PacketPayloadTypeBuilder().setPacketType(PacketType.Raw).setPayloadType(1).build();
  }

  @Test
  public void testIsListenerSubscribedByNotificationTypeWithoutAddingAnyListener() {
    assertEquals(false, packetNotificationRegistry.isListenerSubscribed(Notification.class));
  }

  @Test
  public void testIsListenerSubscribedByEtherTypeWithoutAddingAnyListener() {
    assertEquals(false, packetNotificationRegistry.isListenerSubscribed(packetPayloadType));
  }

  @Test
  public void testIsListenerSubscribedByNotificationTypeSunnyDay() {
    addNotification();
    assertEquals(true, packetNotificationRegistry.isListenerSubscribed(Notification.class));
  }

  @Test
  public void testIsListenerSubscribedByEtherTypeWithoutAddingEtherType() {
    addNotification();
    assertEquals(false, packetNotificationRegistry.isListenerSubscribed(packetPayloadType));
  }

  @Test
  public void testIsListenerSubscribedByEtherTypeSunnyDay() {
    addNotification();
    addEtherTypeNotification();
    assertEquals(true, packetNotificationRegistry.isListenerSubscribed(packetPayloadType));
  }

  private void addNotification() {
    packetNotificationRegistry.onNotificationSubscribtion(Notification.class);

  }

  private void addEtherTypeNotification() {
    packetNotificationRegistry.trackPacketNotificationListener(packetPayloadType, Notification.class);
  }
}
