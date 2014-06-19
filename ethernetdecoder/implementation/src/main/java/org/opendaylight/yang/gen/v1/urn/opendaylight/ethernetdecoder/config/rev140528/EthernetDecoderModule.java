package org.opendaylight.yang.gen.v1.urn.opendaylight.ethernetdecoder.config.rev140528;

import org.opendaylight.l2switch.ethernetdecoder.EthernetDecoder;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ethernet decoder implementation definition
 */
public class EthernetDecoderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.ethernetdecoder.config.rev140528.AbstractEthernetDecoderModule {
  private static final Logger _logger = LoggerFactory.getLogger(EthernetDecoderModule.class);

  public EthernetDecoderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
    super(identifier, dependencyResolver);
  }

  public EthernetDecoderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.ethernetdecoder.config.rev140528.EthernetDecoderModule oldModule, java.lang.AutoCloseable oldInstance) {
    super(identifier, dependencyResolver, oldModule, oldInstance);
  }

  @Override
  public void customValidation() {
    // add custom validation form module attributes here.
  }

  @Override
  public java.lang.AutoCloseable createInstance() {
    // TODO:implement
    PacketDecoderService packetDecoderService = getPacketDecoderServiceDependency();
    packetDecoderService.registerDecoder(getEthernetArpPacketPayloadType(), new EthernetDecoder(), EthernetPacketReceived.class);
    _logger.info("Registered ethernet decoder successfully.");

    return new AutoCloseable() {
      @Override
      public void close() throws Exception {

      }
    };
  }

  private PacketPayloadType getEthernetArpPacketPayloadType() {
    return new PacketPayloadTypeBuilder()
        .setPacketType(PacketType.Raw)
        .setPayloadType(PacketType.Ethernet.getIntValue())
        .build();
  }

}
