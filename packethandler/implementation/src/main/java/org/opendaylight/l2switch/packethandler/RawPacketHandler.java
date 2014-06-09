package org.opendaylight.l2switch.packethandler;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.packet.BufferException;
import org.opendaylight.l2switch.packethandler.decoders.DecoderRegistry;
import org.opendaylight.l2switch.packethandler.decoders.EthernetDecoder;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoder;
import org.opendaylight.l2switch.packethandler.decoders.PacketNotificationRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
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

    EthernetPacket ethernetPacket = null;
    try{
      ethernetPacket = EthernetDecoder.decode(packetReceived);
    }catch(BufferException be){
      _logger.info("EthernetDecoder Failed on {} packet received.",packetReceived);
      throw new RuntimeException("EthernetDecode Failed.",be);
    }

    if(ethernetPacket==null) {
      _logger.info("RawPacket could not be decoded as Ethernet Packet.");
      return;
    }

    // publish ethernet notification if listener is subscribed
    if(packetNotificationRegistry.isListenerSubscribed(EthernetPacketReceived.class)) {
      EthernetPacketReceivedBuilder ethernetPacketReceivedBuilder = new EthernetPacketReceivedBuilder(ethernetPacket);
      EthernetPacketReceived ethernetNotification = ethernetPacketReceivedBuilder.build();
      notificationProviderService.publish(ethernetNotification);
    }

    if(!packetNotificationRegistry.isListenerSubscribed(ethernetPacket.getEthertype())) {
      _logger.info("No Listener is subscribed for {} notification.",ethernetPacket.getEthertype());
      return;
    }

    PacketDecoder packetDecoder = decoderRegistry.getDecoder(ethernetPacket.getEthertype());

    if(packetDecoder==null) {
      _logger.info("No Decode is available for {} packet.",ethernetPacket.getEthertype());
      return;
    }

    EthernetPacketGrp decodedEthernetPacket;

    decodedEthernetPacket = packetDecoder.decode(ethernetPacket);

    Notification packetInNotification = packetDecoder.buildPacketNotification(decodedEthernetPacket);

    notificationProviderService.publish(packetInNotification);
  }

}
