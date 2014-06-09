package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Created by amitmandke on 6/4/14.
 */
public interface PacketDecoderService extends BindingAwareService {
  public <C extends Notification> void registerDecoder(KnownEtherType etherType, PacketDecoder packetDecoder, Class<C> packetReceivedNotificationType);
}
