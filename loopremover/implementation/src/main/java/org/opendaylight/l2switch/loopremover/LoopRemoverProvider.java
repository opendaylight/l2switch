package org.opendaylight.l2switch.loopremover;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphImpl;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphService;
import org.opendaylight.l2switch.loopremover.topology.TopologyLinkDataChangeHandler;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by amitmandke on 7/9/14.
 */
public class LoopRemoverProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(LoopRemoverProvider.class);
  private Registration listenerRegistration;
  private TopologyLinkDataChangeHandler topologyLinkDataChangeHandler;

  /**
   * Setup the L2Switch.
   *
   * @param consumerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    // Setup FlowWriterService
    DataBroker dataService = consumerContext.<DataBroker>getSALService(DataBroker.class);
    NetworkGraphService networkGraphService = new NetworkGraphImpl();

    // Register Topology DataChangeListener
    this.topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataService, networkGraphService);
    listenerRegistration = topologyLinkDataChangeHandler.registerAsDataChangeListener();

  }

  /**
   * Cleanup the L2Switch
   */
  @Override
  public void close() throws Exception {
    if(listenerRegistration != null) {
      listenerRegistration.close();
    }
  }
}
