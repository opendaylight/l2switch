package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packethandler.packet.rev140528.packet.PacketPayloadType;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Created by amitmandke on 6/5/14.
 */
public class PacketDecoderServiceImpl implements PacketDecoderService {
  private DecoderRegistry decoderRegistry;
  private PacketNotificationRegistry packetNotificationRegistry;

  public PacketDecoderServiceImpl(DecoderRegistry decoderRegistry, PacketNotificationRegistry packetNotificationRegistry) {
    this.decoderRegistry = decoderRegistry;
    this.packetNotificationRegistry = packetNotificationRegistry;
  }

  @Override
  public <C extends Notification> void registerDecoder(PacketPayloadType packetPayloadType, PacketDecoder packetDecoder, Class<C> packetReceivedNotificationType) {
    decoderRegistry.addDecoder(packetPayloadType, packetDecoder);
    packetNotificationRegistry.trackPacketNotificationListener(packetPayloadType, packetReceivedNotificationType);
  }
}
