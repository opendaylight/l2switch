/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All
 * rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTrackerConsumer extends AbstractBindingAwareConsumer
        implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HostTrackerConsumer.class);

    HostTrackerImpl mdHostTrackerImpl;
    SimpleAddressObserver simpleAddressObserver;

    public HostTrackerConsumer() {
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        log.trace("onSessionInitialized");
        DataBroker dataService = session.<DataBroker>getSALService(DataBroker.class);
        NotificationService notificationService
                = session.<NotificationService>getSALService(NotificationService.class);
        //ITopologyManager topologyManager = (ITopologyManager) ServiceHelper.getInstance(ITopologyManager.class, GlobalConstants.DEFAULT.toString(), this);
        mdHostTrackerImpl = new HostTrackerImpl(dataService);
        mdHostTrackerImpl.registerAsDataChangeListener();
        simpleAddressObserver = new SimpleAddressObserver(mdHostTrackerImpl, notificationService);
//        simpleAddressObserver.registerAsNotificationListener();
    }

    @Override
    public void close() throws Exception {
        if (mdHostTrackerImpl != null) {
            mdHostTrackerImpl.close();
        }
    }

}
