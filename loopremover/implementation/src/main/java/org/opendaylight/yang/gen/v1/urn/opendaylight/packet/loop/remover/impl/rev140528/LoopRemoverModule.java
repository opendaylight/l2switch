package org.opendaylight.yang.gen.v1.urn.opendaylight.packet.loop.remover.impl.rev140528;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.l2switch.loopremover.flow.InitialFlowWriter;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphImpl;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphService;
import org.opendaylight.l2switch.loopremover.topology.TopologyLinkDataChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopRemoverModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.packet.loop.remover.impl.rev140528.AbstractLoopRemoverModule {

  private final static Logger _logger = LoggerFactory.getLogger(LoopRemoverModule.class);
  private Registration listenerRegistration = null, invListenerReg = null;
  private TopologyLinkDataChangeHandler topologyLinkDataChangeHandler;
  private ListenerRegistration<DataChangeListener> dsListenerReg;

  public LoopRemoverModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
    super(identifier, dependencyResolver);
  }

  public LoopRemoverModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.packet.loop.remover.impl.rev140528.LoopRemoverModule oldModule, java.lang.AutoCloseable oldInstance) {
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

    //Write initial flows
    if (getIsInstallLldpFlow()) {
      _logger.info("LoopRemover will install an lldp flow");
      InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
      initialFlowWriter.setFlowTableId(getLldpFlowTableId());
      initialFlowWriter.setFlowPriority(getLldpFlowPriority());
      initialFlowWriter.setFlowIdleTimeout(getLldpFlowIdleTimeout());
      initialFlowWriter.setFlowHardTimeout(getLldpFlowHardTimeout());
      invListenerReg = notificationService.registerNotificationListener(initialFlowWriter);
      InstanceIdentifier<FlowCapableNode> nodePath = InstanceIdentifier.create(Nodes.class)
              .child(Node.class).augmentation(FlowCapableNode.class);
      dsListenerReg = dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, nodePath,
              initialFlowWriter, AsyncDataBroker.DataChangeScope.BASE);
    }

    // Register Topology DataChangeListener
    NetworkGraphService networkGraphService = new NetworkGraphImpl();
    this.topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataService, networkGraphService);
    topologyLinkDataChangeHandler.setGraphRefreshDelay(getGraphRefreshDelay());
    topologyLinkDataChangeHandler.setTopologyId(getTopologyId());
    listenerRegistration = topologyLinkDataChangeHandler.registerAsDataChangeListener();

    final class CloseResources implements AutoCloseable {
      @Override
      public void close() throws Exception {
        if(listenerRegistration != null) {
          listenerRegistration.close();
        }
        if(invListenerReg != null) {
          invListenerReg.close();
        }
        if (dsListenerReg != null) {
          dsListenerReg.close();
        }
        _logger.info("LoopRemover (instance {}) torn down.", this);
      }
    }
    AutoCloseable ret = new CloseResources();
    _logger.info("LoopRemover (instance {}) initialized.", ret);
    return ret;

  }

}
