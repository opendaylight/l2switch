package org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.main.impl.rev140528;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.l2switch.flow.FlowWriterServiceImpl;
import org.opendaylight.l2switch.flow.InitialFlowWriter;
import org.opendaylight.l2switch.flow.ReactiveFlowWriter;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2SwitchMainModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.main.impl.rev140528.AbstractL2SwitchMainModule {

  private final static Logger _logger = LoggerFactory.getLogger(L2SwitchMainModule.class);
  private Registration invListenerReg = null, reactFlowWriterReg = null;

  public L2SwitchMainModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
    super(identifier, dependencyResolver);
  }

  public L2SwitchMainModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.main.impl.rev140528.L2SwitchMainModule oldModule, java.lang.AutoCloseable oldInstance) {
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

    // Setup FlowWrtierService
    FlowWriterServiceImpl flowWriterService = new FlowWriterServiceImpl(salFlowService);
    flowWriterService.setFlowTableId(getReactiveFlowTableId());
    flowWriterService.setFlowPriority(getReactiveFlowPriority());
    flowWriterService.setFlowIdleTimeout(getReactiveFlowIdleTimeout());
    flowWriterService.setFlowHardTimeout(getReactiveFlowHardTimeout());

    // Setup InventoryReader
    InventoryReader inventoryReader = new InventoryReader(dataService);

    // Write initial flows
    if (getIsInstallDropallFlow()) {
      _logger.info("L2Switch will install a dropall flow on each switch");
      InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
      initialFlowWriter.setFlowTableId(getDropallFlowTableId());
      initialFlowWriter.setFlowPriority(getDropallFlowPriority());
      initialFlowWriter.setFlowIdleTimeout(getDropallFlowIdleTimeout());
      initialFlowWriter.setFlowHardTimeout(getDropallFlowHardTimeout());
      invListenerReg = notificationService.registerNotificationListener(initialFlowWriter);
    }
    else {
      _logger.info("Dropall flows will not be installed");
    }

    if (getIsLearningOnlyMode()) {
      _logger.info("L2Switch is in Learning Only Mode");
    }
    else {
      // Setup reactive flow writer
      _logger.info("L2Switch will react to network traffic and install flows");
      ReactiveFlowWriter reactiveFlowWriter = new ReactiveFlowWriter(inventoryReader, flowWriterService);
      reactFlowWriterReg = notificationService.registerNotificationListener(reactiveFlowWriter);
    }

    final class CloseResources implements AutoCloseable {
      @Override
      public void close() throws Exception {
        if(reactFlowWriterReg != null) {
          reactFlowWriterReg.close();
        }
        if(invListenerReg != null) {
          invListenerReg.close();
        }
        _logger.info("L2SwitchMain (instance {}) torn down.", this);
      }
    }
    AutoCloseable ret = new CloseResources();
    _logger.info("L2SwitchMain (instance {}) initialized.", ret);
    return ret;
  }

}
