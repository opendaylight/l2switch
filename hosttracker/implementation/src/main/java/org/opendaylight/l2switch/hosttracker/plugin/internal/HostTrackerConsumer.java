package org.opendaylight.l2switch.hosttracker.plugin.internal;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author aanm
 * @version 0.0.1
 */
public class HostTrackerConsumer extends AbstractBindingAwareConsumer
        implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HostTrackerConsumer.class);

    HostTrackerImpl mdHostTrackerImpl;

    public HostTrackerConsumer() {
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        log.trace("onSessionInitialized");
        DataBroker dataService = session.<DataBroker>getSALService(DataBroker.class);
        mdHostTrackerImpl = new HostTrackerImpl(dataService);
        mdHostTrackerImpl.registerAsDataChangeListener();
    }

    @Override
    public void close() throws Exception {
        if (mdHostTrackerImpl != null) {
            mdHostTrackerImpl.close();
        }
    }

}
