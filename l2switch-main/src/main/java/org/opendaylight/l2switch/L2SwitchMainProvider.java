package org.opendaylight.l2switch;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.flow.FlowWriterServiceImpl;
import org.opendaylight.l2switch.flow.InitialFlowWriter;
import org.opendaylight.l2switch.flow.ReactiveFlowWriter;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.main.config.rev140528.MainConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2SwitchMainProvider {

    private final static Logger LOG = LoggerFactory.getLogger(L2SwitchMainProvider.class);
    private Registration topoNodeListherReg = null, reactFlowWriterReg = null;

    private final DataBroker dataService;
    private final NotificationProviderService notificationService;
    private final SalFlowService salFlowService;
    private final MainConfig mainConfig;

    public L2SwitchMainProvider(final DataBroker dataBroker,
            final NotificationProviderService notificationService,
            final SalFlowService salFlowService, final MainConfig config) {
        this.dataService = dataBroker;
        this.notificationService = notificationService;
        this.salFlowService = salFlowService;
        this.mainConfig = config;
    }

    public void init() {
        // Setup FlowWrtierService
        FlowWriterServiceImpl flowWriterService = new FlowWriterServiceImpl(salFlowService);
        flowWriterService.setFlowTableId(mainConfig.getReactiveFlowTableId());
        flowWriterService.setFlowPriority(mainConfig.getReactiveFlowPriority());
        flowWriterService.setFlowIdleTimeout(mainConfig.getReactiveFlowIdleTimeout());
        flowWriterService.setFlowHardTimeout(mainConfig.getReactiveFlowHardTimeout());

        // Setup InventoryReader
        InventoryReader inventoryReader = new InventoryReader(dataService);

        // Write initial flows
        if (mainConfig.isIsInstallDropallFlow()) {
            LOG.info("L2Switch will install a dropall flow on each switch");
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
            initialFlowWriter.setFlowTableId(mainConfig.getDropallFlowTableId());
            initialFlowWriter.setFlowPriority(mainConfig.getDropallFlowPriority());
            initialFlowWriter.setFlowIdleTimeout(mainConfig.getDropallFlowIdleTimeout());
            initialFlowWriter.setFlowHardTimeout(mainConfig.getDropallFlowHardTimeout());
            topoNodeListherReg = initialFlowWriter.registerAsDataChangeListener(dataService);
        }
        else {
            LOG.info("Dropall flows will not be installed");
        }

        if (mainConfig.isIsLearningOnlyMode()) {
            LOG.info("L2Switch is in Learning Only Mode");
        }
        else {
            // Setup reactive flow writer
            LOG.info("L2Switch will react to network traffic and install flows");
            ReactiveFlowWriter reactiveFlowWriter = new ReactiveFlowWriter(inventoryReader, flowWriterService);
            reactFlowWriterReg = notificationService.registerNotificationListener(reactiveFlowWriter);
        }

        LOG.info("L2SwitchMain initialized.");
    }

    public void close() throws Exception {
        if(reactFlowWriterReg != null) {
            reactFlowWriterReg.close();
        }
        if(topoNodeListherReg != null) {
            topoNodeListherReg.close();
        }
        LOG.info("L2SwitchMain (instance {}) torn down.", this);
    }

}
