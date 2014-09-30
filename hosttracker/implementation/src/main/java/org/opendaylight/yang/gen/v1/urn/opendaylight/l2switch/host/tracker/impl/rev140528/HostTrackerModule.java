package org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.host.tracker.impl.rev140528;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.hosttracker.plugin.internal.HostTrackerImpl;
import org.opendaylight.l2switch.hosttracker.plugin.internal.SimpleAddressObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTrackerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.host.tracker.impl.rev140528.AbstractHostTrackerModule {

    private static final Logger log = LoggerFactory.getLogger(HostTrackerModule.class);

    HostTrackerImpl mdHostTrackerImpl;
    SimpleAddressObserver simpleAddressObserver;

    public HostTrackerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public HostTrackerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.host.tracker.impl.rev140528.HostTrackerModule oldModule, java.lang.AutoCloseable oldInstance) {
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

        //ITopologyManager topologyManager = (ITopologyManager) ServiceHelper.getInstance(ITopologyManager.class, GlobalConstants.DEFAULT.toString(), this);
        mdHostTrackerImpl = new HostTrackerImpl(dataService, getTopologyId());
        mdHostTrackerImpl.registerAsDataChangeListener();
        //simpleAddressObserver = new SimpleAddressObserver(mdHostTrackerImpl, notificationService);
        //simpleAddressObserver.registerAsNotificationListener();

        final class CloseResources implements AutoCloseable {
            @Override
            public void close() throws Exception {
                if(mdHostTrackerImpl != null) {
                    mdHostTrackerImpl.close();
                }
                log.info("HostTracker (instance {}) torn down.", this);
            }
        }
        AutoCloseable ret = new CloseResources();
        log.info("HostTracker (instance {}) initialized.", ret);
        return ret;
    }

}
