package org.opendaylight.yang.gen.v1.urn.opendaylight.packetdecoder.config.impl.rev140528;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.packethandler.RawPacketHandler;
import org.opendaylight.l2switch.packethandler.decoders.DecoderRegistry;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoderServiceImpl;
import org.opendaylight.l2switch.packethandler.decoders.PacketNotificationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketDecoderImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.packetdecoder.config.impl.rev140528.AbstractPacketDecoderImplModule {
  private static final Logger _logger = LoggerFactory.getLogger(PacketDecoderImplModule.class);

  public PacketDecoderImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PacketDecoderImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.packetdecoder.config.impl.rev140528.PacketDecoderImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
      NotificationProviderService notificationProviderService = getNotificationServiceDependency();
      DecoderRegistry decoderRegistry = new DecoderRegistry();

      PacketNotificationRegistry packetNotificationRegistry = new PacketNotificationRegistry();
      notificationProviderService.registerInterestListener(packetNotificationRegistry);


      RawPacketHandler rawPacketHandler = new RawPacketHandler();
      rawPacketHandler.setNotificationProviderService(notificationProviderService);
      rawPacketHandler.setDecoderRegistry(decoderRegistry);
      rawPacketHandler.setPacketNotificationRegistry(packetNotificationRegistry);
      notificationProviderService.registerNotificationListener(rawPacketHandler);
      _logger.info("About to return packet decoder instance.");
      PacketDecoderServiceImpl packetDecoderService = new PacketDecoderServiceImpl(decoderRegistry, packetNotificationRegistry);

      final PacketDecoderImplRuntimeRegistration runtimeRegistration =  getRootRuntimeBeanRegistratorWrapper().register(packetDecoderService);
      AutoCloseable autoCloseable = new AutoCloseable() {
        @Override
        public void close() throws Exception {
          runtimeRegistration.close();
        }
      };
      return autoCloseable;

    }

}
