/*
 * Copyright (c) 2016 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import org.opendaylight.l2switch.arphandler.flow.InitialFlowWriter;
import org.opendaylight.l2switch.arphandler.inventory.InventoryReader;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.handler.config.rev140528.ArpHandlerConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArpHandlerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ArpHandlerProvider.class);
    private Registration listenerRegistration;
    private Registration floodTopoListenerReg;
    private Registration topoNodeListenerReg;

    private final NotificationService notificationService;
    private final DataBroker dataBroker;
    private final SalFlowService salFlowService;
    private final PacketProcessingService packetProcessingService;
    private final ArpHandlerConfig arpHandlerConfig;

    public ArpHandlerProvider(final DataBroker dataBroker,
            final NotificationService notificationProviderService,
            final SalFlowService salFlowService,
            final PacketProcessingService packetProcessingService,
            final ArpHandlerConfig config) {
        this.notificationService = notificationProviderService;
        this.dataBroker = dataBroker;
        this.salFlowService = salFlowService;
        this.packetProcessingService = packetProcessingService;
        this.arpHandlerConfig = config;
    }

    public void init() {
        if (arpHandlerConfig.getIsProactiveFloodMode()) {
            //Setup proactive flow writer, which writes flood flows
            LOG.info("ArpHandler is in Proactive Flood Mode");
            ProactiveFloodFlowWriter floodFlowWriter = new ProactiveFloodFlowWriter(dataBroker, salFlowService);
            floodFlowWriter.setFlowTableId(arpHandlerConfig.getFloodFlowTableId());
            floodFlowWriter.setFlowPriority(arpHandlerConfig.getFloodFlowPriority());
            floodFlowWriter.setFlowIdleTimeout(arpHandlerConfig.getFloodFlowIdleTimeout());
            floodFlowWriter.setFlowHardTimeout(arpHandlerConfig.getFloodFlowHardTimeout());
            floodFlowWriter.setFlowInstallationDelay(arpHandlerConfig.getFloodFlowInstallationDelay().toJava());
            floodTopoListenerReg = floodFlowWriter.registerAsDataChangeListener();
        } else {
            //Write initial flows to send arp to controller
            LOG.info("ArpHandler is in Reactive Mode");
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
            initialFlowWriter.setFlowTableId(arpHandlerConfig.getArpFlowTableId());
            initialFlowWriter.setFlowPriority(arpHandlerConfig.getArpFlowPriority());
            initialFlowWriter.setFlowIdleTimeout(arpHandlerConfig.getArpFlowIdleTimeout());
            initialFlowWriter.setFlowHardTimeout(arpHandlerConfig.getArpFlowHardTimeout());
            initialFlowWriter.setIsHybridMode(arpHandlerConfig.getIsHybridMode());
            topoNodeListenerReg = initialFlowWriter.registerAsDataChangeListener(dataBroker);

            // Setup InventoryReader
            InventoryReader inventoryReader = new InventoryReader(dataBroker);

            // Setup PacketDispatcher
            PacketDispatcher packetDispatcher = new PacketDispatcher(inventoryReader, packetProcessingService);

            // Setup ArpPacketHandler
            ArpPacketHandler arpPacketHandler = new ArpPacketHandler(packetDispatcher);

            // Register ArpPacketHandler
            this.listenerRegistration = notificationService.registerListener(ArpPacketReceived.class, arpPacketHandler);
        }
        LOG.info("ArpHandler initialized.");
    }

    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
        if (floodTopoListenerReg != null) {
            floodTopoListenerReg.close();
        }
        if (topoNodeListenerReg != null) {
            topoNodeListenerReg.close();
        }
        LOG.info("ArpHandler (instance {}) torn down.", this);
    }
}
