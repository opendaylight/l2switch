package org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.handler.impl.rev140528;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.l2switch.arphandler.core.ArpPacketHandler;
import org.opendaylight.l2switch.arphandler.core.PacketDispatcher;
import org.opendaylight.l2switch.arphandler.core.ProactiveFloodFlowWriter;
import org.opendaylight.l2switch.arphandler.flow.InitialFlowWriter;
import org.opendaylight.l2switch.arphandler.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArpHandlerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.handler.impl.rev140528.AbstractArpHandlerModule {

  private final static Logger _logger = LoggerFactory.getLogger(ArpHandlerModule.class);
  private Registration listenerRegistration = null, floodTopoListenerReg = null, floodInvListenerReg = null, invListenerReg = null;

  public ArpHandlerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
    super(identifier, dependencyResolver);
  }

  public ArpHandlerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.handler.impl.rev140528.ArpHandlerModule oldModule, java.lang.AutoCloseable oldInstance) {
    super(identifier, dependencyResolver, oldModule, oldInstance);
  }

  @Override
  public void customValidation() {
    // add custom validation form module attributes here.
  }

  @Override
  public java.lang.AutoCloseable createInstance() {
    NotificationProviderService notificationService = getNotificationServiceDependency();
    DataBroker dataService = getDataBrokerDependency();
    RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency();
    SalFlowService salFlowService = rpcRegistryDependency.getRpcService(SalFlowService.class);

    if(getIsProactiveFloodMode()) {
      //Setup proactive flow writer, which writes flood flows
      _logger.info("ArpHandler is in Proactive Flood Mode");
      ProactiveFloodFlowWriter floodFlowWriter = new ProactiveFloodFlowWriter(dataService, salFlowService);
      floodFlowWriter.setFlowTableId(getFloodFlowTableId());
      floodFlowWriter.setFlowPriority(getFloodFlowPriority());
      floodFlowWriter.setFlowIdleTimeout(getFloodFlowIdleTimeout());
      floodFlowWriter.setFlowHardTimeout(getFloodFlowHardTimeout());
      floodFlowWriter.setFlowInstallationDelay(getFloodFlowInstallationDelay());
      floodTopoListenerReg = floodFlowWriter.registerAsDataChangeListener();
      floodInvListenerReg = notificationService.registerNotificationListener(floodFlowWriter);
    } else {

      //Write initial flows to send arp to controller
      _logger.info("ArpHandler is in Reactive Mode");
      InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
      initialFlowWriter.setFlowTableId(getArpFlowTableId());
      initialFlowWriter.setFlowPriority(getArpFlowPriority());
      initialFlowWriter.setFlowIdleTimeout(getArpFlowIdleTimeout());
      initialFlowWriter.setFlowHardTimeout(getArpFlowHardTimeout());
      invListenerReg = notificationService.registerNotificationListener(initialFlowWriter);

      // Setup InventoryReader
      InventoryReader inventoryReader = new InventoryReader(dataService);

      // Setup PacketDispatcher
      PacketProcessingService packetProcessingService =
        rpcRegistryDependency.<PacketProcessingService>getRpcService(PacketProcessingService.class);
      PacketDispatcher packetDispatcher = new PacketDispatcher();
      packetDispatcher.setInventoryReader(inventoryReader);
      packetDispatcher.setPacketProcessingService(packetProcessingService);

      // Setup ArpPacketHandler
      ArpPacketHandler arpPacketHandler = new ArpPacketHandler(packetDispatcher);

      // Register ArpPacketHandler
      this.listenerRegistration = notificationService.registerNotificationListener(arpPacketHandler);
    }

    final class CloseResources implements AutoCloseable {
      @Override
      public void close() throws Exception {
        if(listenerRegistration != null) {
          listenerRegistration.close();
        }
        if(floodTopoListenerReg != null) {
          floodTopoListenerReg.close();
        }
        if(floodInvListenerReg != null) {
          floodInvListenerReg.close();
        }
        if(invListenerReg != null) {
          invListenerReg.close();
        }
        _logger.info("ArpHandler (instance {}) torn down.", this);
      }
    }
    AutoCloseable ret = new CloseResources();
    _logger.info("ArpHandler (instance {}) initialized.", ret);
    return ret;
  }

  /**
   * Reads config subsystem to determine the flood mode (proactive or reactive)
   * @return True if the flood mode is proactive
   */
  private boolean isProactiveFloodMode() {
    // Config Subsystem integration to be done
    // For now, proactive mode is always enabled for performance
    return true;
  }

}
