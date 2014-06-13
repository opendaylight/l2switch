package org.opendaylight.l2switch.packethandler;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.packethandler.decoders.DecoderRegistry;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoder;
import org.opendaylight.l2switch.packethandler.decoders.PacketNotificationRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.BasePacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.BasePacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RawPacketHandler subscribes to RawPacket in event. Calls the Ethernet decoder to
 */
public class RawPacketHandler implements PacketProcessingListener {
  private final static Logger _logger = LoggerFactory.getLogger(RawPacketHandler.class);

  private NotificationProviderService notificationProviderService;
  private PacketNotificationRegistry packetNotificationRegistry;
  private DecoderRegistry decoderRegistry;

  public void setDecoderRegistry(DecoderRegistry decoderRegistry) {
    this.decoderRegistry = decoderRegistry;
  }

  public void setPacketNotificationRegistry(PacketNotificationRegistry packetNotificationRegistry) {
    this.packetNotificationRegistry = packetNotificationRegistry;
  }


  public void setNotificationProviderService(NotificationProviderService notificationProviderService) {
    this.notificationProviderService = notificationProviderService;
  }

  @Override
  public void onPacketReceived(PacketReceived packetReceived) {

    if(packetReceived == null) return;

    PacketPayloadType packetPayloadType = getPacketPayloadType(packetReceived);
    PacketDecoder packetDecoder = decoderRegistry.getDecoder(packetPayloadType);
    Packet packet = getBasePacket(packetReceived);

    while(packetDecoder != null) {

      packet = packetDecoder.decode(packet);

      if(packet == null) {
        _logger.info("Could not decode packet : []", packet);
        break;
      }

      Notification packetInNotification = packetDecoder.buildPacketNotification(packet);
      if(packetInNotification != null && packetNotificationRegistry.isListenerSubscribed(packetInNotification.getClass()))
        notificationProviderService.publish(packetInNotification);

      packetPayloadType = packet.getPacketPayloadType();
      if(packetPayloadType == null) {
        _logger.info("No PacketPayloadType set in packet : []", packet);
        break;
      }

      packetDecoder = decoderRegistry.getDecoder(packetPayloadType);
    }
  }

  private BasePacket getBasePacket(PacketReceived packetReceived) {

    return new BasePacketBuilder()
        .setPacketPayloadType(getPacketPayloadType(packetReceived))
        .setRawPacket(getRawPacket(packetReceived)).build();
  }

  private PacketPayloadType getPacketPayloadType(PacketReceived packetReceived) {

    //currently doesn't make use of packet received as currently only ethernet packets are received so following is hard coded.
    return new PacketPayloadTypeBuilder().setPacketType(PacketType.Raw).setPayloadType(PacketType.Ethernet.getIntValue()).build();
  }

  private RawPacket getRawPacket(PacketReceived packetReceived) {
    return new RawPacketBuilder().setIngress(packetReceived.getIngress()).setPayload(packetReceived.getPayload()).build();
  }

}
