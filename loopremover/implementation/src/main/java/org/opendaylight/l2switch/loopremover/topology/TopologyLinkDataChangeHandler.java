/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.topology;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.l2switch.loopremover.util.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to data change events on topology links {@link
 * org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
 * and maintains a topology graph using provided NetworkGraphService
 * {@link org.opendaylight.l2switch.loopremover.topology.NetworkGraphService}.
 * It refreshes the graph after a delay(default 10 sec) to accommodate burst of
 * change events if they come in bulk. This is to avoid continuous refresh of
 * graph on a series of change events in short time.
 */
public class TopologyLinkDataChangeHandler implements DataTreeChangeListener<Link> {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyLinkDataChangeHandler.class);
    private static final String DEFAULT_TOPOLOGY_ID = "flow:1";
    private static final long DEFAULT_GRAPH_REFRESH_DELAY = 1000;

    private final ScheduledExecutorService topologyDataChangeEventProcessor = Executors.newScheduledThreadPool(1);

    private final NetworkGraphService networkGraphService;
    private volatile boolean networkGraphRefreshScheduled = false;
    private volatile boolean threadReschedule = false;
    private long graphRefreshDelay;
    private String topologyId;

    private final DataBroker dataBroker;

    public TopologyLinkDataChangeHandler(DataBroker dataBroker, NetworkGraphService networkGraphService) {
        Preconditions.checkNotNull(dataBroker, "dataBroker should not be null.");
        Preconditions.checkNotNull(networkGraphService, "networkGraphService should not be null.");
        this.dataBroker = dataBroker;
        this.networkGraphService = networkGraphService;
    }

    public void setGraphRefreshDelay(long graphRefreshDelay) {
        if (graphRefreshDelay < 0) {
            this.graphRefreshDelay = DEFAULT_GRAPH_REFRESH_DELAY;
        } else {
            this.graphRefreshDelay = graphRefreshDelay;
        }
    }

    public void setTopologyId(String topologyId) {
        if (topologyId == null || topologyId.isEmpty()) {
            this.topologyId = DEFAULT_TOPOLOGY_ID;
        } else {
            this.topologyId = topologyId;
        }
    }

    /**
     * Registers as a data listener to receive changes done to {@link org.opendaylight.yang.gen.v1.urn.tbd.params
     * .xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
     * under
     * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology}
     * operation data root.
     */
    public ListenerRegistration<TopologyLinkDataChangeHandler> registerAsDataChangeListener() {
        InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId))).child(Link.class).build();
        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, linkInstance), this);
    }

    /**
     * Handler for onDataChanged events and schedules the building of the
     * network graph.
     */
    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Link>> changes) {
        boolean isGraphUpdated = false;

        for (DataTreeModification<Link> change: changes) {
            DataObjectModification<Link> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    Link createdLink = rootNode.getDataAfter();
                    if (rootNode.getDataBefore() == null && !createdLink.getLinkId().getValue().contains("host")) {
                        isGraphUpdated = true;
                        LOG.debug("Graph is updated! Added Link {}", createdLink.getLinkId().getValue());
                    }
                    break;
                case DELETE:
                    Link deletedLink = rootNode.getDataBefore();
                    if (!deletedLink.getLinkId().getValue().contains("host")) {
                        isGraphUpdated = true;
                        LOG.debug("Graph is updated! Removed Link {}", deletedLink.getLinkId().getValue());
                        break;
                    }
                    break;
                default:
                    break;
            }
        }

        if (!isGraphUpdated) {
            return;
        }
        if (!networkGraphRefreshScheduled) {
            synchronized (this) {
                if (!networkGraphRefreshScheduled) {
                    topologyDataChangeEventProcessor.schedule(new TopologyDataChangeEventProcessor(), graphRefreshDelay,
                            TimeUnit.MILLISECONDS);
                    networkGraphRefreshScheduled = true;
                    LOG.debug("Scheduled Graph for refresh.");
                }
            }
        } else {
            LOG.debug("Already scheduled for network graph refresh.");
            threadReschedule = true;
        }
    }

    private class TopologyDataChangeEventProcessor implements Runnable {

        @Override
        public void run() {
            if (threadReschedule) {
                topologyDataChangeEventProcessor.schedule(this, graphRefreshDelay, TimeUnit.MILLISECONDS);
                threadReschedule = false;
                return;
            }
            LOG.debug("In network graph refresh thread.");
            networkGraphRefreshScheduled = false;
            networkGraphService.clear();
            List<Link> links = getLinksFromTopology();
            if (links == null || links.isEmpty()) {
                return;
            }
            networkGraphService.addLinks(links);
            final ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
            updateNodeConnectorStatus(readWriteTransaction);
            Futures.addCallback(readWriteTransaction.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void notUsed) {
                    LOG.debug("TopologyLinkDataChangeHandler write successful for tx :{}",
                            readWriteTransaction.getIdentifier());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    LOG.error("TopologyLinkDataChangeHandler write transaction {} failed",
                            readWriteTransaction.getIdentifier(), throwable.getCause());
                }
            }, MoreExecutors.directExecutor());
            LOG.debug("Done with network graph refresh thread.");
        }

        private List<Link> getLinksFromTopology() {
            InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifierUtils
                    .generateTopologyInstanceIdentifier(topologyId);
            Topology topology = null;
            ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
            try {
                Optional<Topology> topologyOptional = readOnlyTransaction
                        .read(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier).get();
                if (topologyOptional.isPresent()) {
                    topology = topologyOptional.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error reading topology {}", topologyInstanceIdentifier);
                readOnlyTransaction.close();
                throw new RuntimeException(
                        "Error reading from operational store, topology : " + topologyInstanceIdentifier, e);
            }
            readOnlyTransaction.close();
            if (topology == null) {
                return null;
            }
            List<Link> links = topology.getLink();
            if (links == null || links.isEmpty()) {
                return null;
            }
            List<Link> internalLinks = new ArrayList<>();
            for (Link link : links) {
                if (!link.getLinkId().getValue().contains("host")) {
                    internalLinks.add(link);
                }
            }
            return internalLinks;
        }

        private void updateNodeConnectorStatus(ReadWriteTransaction readWriteTransaction) {
            List<Link> allLinks = networkGraphService.getAllLinks();
            if (allLinks == null || allLinks.isEmpty()) {
                return;
            }

            List<Link> mstLinks = networkGraphService.getLinksInMst();
            for (Link link : allLinks) {
                if (mstLinks != null && !mstLinks.isEmpty() && mstLinks.contains(link)) {
                    updateNodeConnector(readWriteTransaction, getSourceNodeConnectorRef(link), StpStatus.Forwarding);
                    updateNodeConnector(readWriteTransaction, getDestNodeConnectorRef(link), StpStatus.Forwarding);
                } else {
                    updateNodeConnector(readWriteTransaction, getSourceNodeConnectorRef(link), StpStatus.Discarding);
                    updateNodeConnector(readWriteTransaction, getDestNodeConnectorRef(link), StpStatus.Discarding);
                }
            }
        }

        private NodeConnectorRef getSourceNodeConnectorRef(Link link) {
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier = InstanceIdentifierUtils
                    .createNodeConnectorIdentifier(link.getSource().getSourceNode().getValue(),
                            link.getSource().getSourceTp().getValue());
            return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
        }

        private NodeConnectorRef getDestNodeConnectorRef(Link link) {
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier = InstanceIdentifierUtils
                    .createNodeConnectorIdentifier(link.getDestination().getDestNode().getValue(),
                            link.getDestination().getDestTp().getValue());

            return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
        }

        private void updateNodeConnector(ReadWriteTransaction readWriteTransaction, NodeConnectorRef nodeConnectorRef,
                StpStatus stpStatus) {
            StpStatusAwareNodeConnectorBuilder stpStatusAwareNodeConnectorBuilder =
                    new StpStatusAwareNodeConnectorBuilder().setStatus(stpStatus);
            checkIfExistAndUpdateNodeConnector(readWriteTransaction, nodeConnectorRef,
                    stpStatusAwareNodeConnectorBuilder.build());
        }

        private void checkIfExistAndUpdateNodeConnector(ReadWriteTransaction readWriteTransaction,
                NodeConnectorRef nodeConnectorRef, StpStatusAwareNodeConnector stpStatusAwareNodeConnector) {
            NodeConnector nc = null;
            try {
                Optional<NodeConnector> dataObjectOptional = readWriteTransaction.read(LogicalDatastoreType.OPERATIONAL,
                        (InstanceIdentifier<NodeConnector>) nodeConnectorRef.getValue()).get();
                if (dataObjectOptional.isPresent()) {
                    nc = dataObjectOptional.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error reading node connector {}", nodeConnectorRef.getValue());
                readWriteTransaction.submit();
                throw new RuntimeException("Error reading from operational store, node connector : " + nodeConnectorRef,
                        e);
            }

            if (nc != null) {
                if (sameStatusPresent(nc.augmentation(StpStatusAwareNodeConnector.class),
                        stpStatusAwareNodeConnector.getStatus())) {
                    return;
                }

                // build instance id for StpStatusAwareNodeConnector
                InstanceIdentifier<StpStatusAwareNodeConnector> stpStatusAwareNcInstanceId =
                        ((InstanceIdentifier<NodeConnector>) nodeConnectorRef
                                .getValue()).augmentation(StpStatusAwareNodeConnector.class);
                // update StpStatusAwareNodeConnector in operational store
                readWriteTransaction.merge(LogicalDatastoreType.OPERATIONAL, stpStatusAwareNcInstanceId,
                        stpStatusAwareNodeConnector);
                LOG.debug("Merged Stp Status aware node connector in operational {} with status {}",
                        stpStatusAwareNcInstanceId, stpStatusAwareNodeConnector);
            } else {
                LOG.error("Unable to update Stp Status node connector {} note present in  operational store",
                        nodeConnectorRef.getValue());
            }
        }

        private boolean sameStatusPresent(StpStatusAwareNodeConnector stpStatusAwareNodeConnector,
                StpStatus stpStatus) {

            if (stpStatusAwareNodeConnector == null) {
                return false;
            }

            if (stpStatusAwareNodeConnector.getStatus() == null) {
                return false;
            }

            if (stpStatus.getIntValue() != stpStatusAwareNodeConnector.getStatus().getIntValue()) {
                return false;
            }

            return true;
        }
    }
}
