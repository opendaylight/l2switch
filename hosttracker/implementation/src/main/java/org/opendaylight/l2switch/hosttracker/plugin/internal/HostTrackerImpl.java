package org.opendaylight.l2switch.hosttracker.plugin.internal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTrackerImpl implements DataChangeListener {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();

    private static final Logger log = LoggerFactory.getLogger(HostTrackerImpl.class);

    private DataBroker dataService;

    private ConcurrentHashMap<HostId, HostNodeBuilder> hosts;

    private AtomicLong id;

    HostTrackerImpl(DataBroker dataService) {
        Preconditions.checkNotNull(dataService, "dataBrokerService should not be null.");
        this.dataService = dataService;
        this.hosts = new ConcurrentHashMap<>();
        this.id = new AtomicLong();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        ExecutorService exec = Executors.newFixedThreadPool(CPUS);
        exec.submit(new Runnable() {
            public void run() {
                if (change == null) {
                    log.info("In onDataChanged: No processing done as change even is null.");
                }
                Map<InstanceIdentifier<?>, DataObject> linkOriginalData = (Map<InstanceIdentifier<?>, DataObject>) change.getOriginalData();
                Map<InstanceIdentifier<?>, DataObject> linkUpdatedData = change.getUpdatedData();
                for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : linkUpdatedData.entrySet()) {
                    InstanceIdentifier<?> key = entrySet.getKey();
                    final DataObject dataObject = entrySet.getValue();
                    if (dataObject instanceof Addresses) {
                        InstanceIdentifier<NodeConnector> iinc = key.firstIdentifierOf(NodeConnector.class);
                        ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction();
                        ListenableFuture<Optional<NodeConnector>> dataFuture = readTx.read(LogicalDatastoreType.OPERATIONAL, iinc);
                        Futures.addCallback(dataFuture, new FutureCallback<Optional<NodeConnector>>() {
                            @Override
                            public void onSuccess(final Optional<NodeConnector> result) {
                                if (result.isPresent()) {
                                    processHost(result, dataObject);
                                }
                            }

                            @Override
                            public void onFailure(Throwable arg0) {
                            }
                        });
                    }
                }
            }
        });
    }

    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void registerAsDataChangeListener() {
        InstanceIdentifier<Addresses> addrCapableNodeConnectors = //
                InstanceIdentifier.builder(Nodes.class) //
                .child(Node.class).child(NodeConnector.class) //
                .augmentation(AddressCapableNodeConnector.class)//
                .child(Addresses.class).build();
        dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, addrCapableNodeConnectors, this, DataChangeScope.SUBTREE);
    }

    private HostNodeBuilder createHost(Addresses addrs, NodeConnector nodeConnector) {
        HostNodeBuilder host = new HostNodeBuilder();
        if (addrs != null) {
            List<Addresses> setAddrs = new ArrayList<>();
            setAddrs.add(addrs);
            host.setAddresses(setAddrs);
            host.setId(new HostId(addrs.getMac().getValue()));
        } else {
            host.setId(new HostId(Long.toString(this.id.getAndIncrement())));
        }
        if (nodeConnector != null) {
//            InstanceIdentifier<NodeConnector> iinc
//                    = InstanceIdentifier.builder(Nodes.class)
//                    .child(Node.class)
//                    .child(NodeConnector.class, nodeConnector.getKey())
//                    .build();
//            NodeConnectorRef ncr = new NodeConnectorRef(iinc);
            List<AttachmentPoints> attachmentPoints = new ArrayList<>();
            attachmentPoints.add(new AttachmentPointsBuilder().setTpId(new TpId(nodeConnector.getId())).build());
            host.setAttachmentPoints(attachmentPoints);
        }
        return host;
    }

    private void updateHost(HostNodeBuilder oldHost, HostNodeBuilder newHost) {
        synchronized (oldHost) {
            oldHost.getAddresses().removeAll(newHost.getAddresses());
            oldHost.getAddresses().addAll(newHost.getAddresses());
            oldHost.getAttachmentPoints().removeAll(newHost.getAttachmentPoints());
            oldHost.getAttachmentPoints().addAll(newHost.getAttachmentPoints());
        }
    }

    private void processHost(Optional<NodeConnector> result, DataObject dataObject) {
        Addresses addrs = (Addresses) dataObject;
        NodeConnector nodeConnector = (NodeConnector) result.get();
        if (!isNodeConnectorInternal(nodeConnector)) {
            HostNodeBuilder host = createHost(addrs, nodeConnector);
            if (hosts.containsKey(host.getId())) {
                updateHost(hosts.get(host.getId()), host);
            } else {
                hosts.put(host.getId(), host);
            }
            writeHosttoMDSAL(hosts.get(host.getId()).build());
        } else {
            log.trace("NodeConnector is internal" + nodeConnector.getId().toString());
        }
    }

    private boolean isNodeConnectorInternal(NodeConnector nodeConnector) {
        TpId tpId = new TpId(nodeConnector.getKey().getId().getValue());
        InstanceIdentifier<NetworkTopology> ntII
                = InstanceIdentifier.builder(NetworkTopology.class).build();
        ReadOnlyTransaction rot = dataService.newReadOnlyTransaction();
        ListenableFuture<Optional<NetworkTopology>> lfONT = rot.read(LogicalDatastoreType.OPERATIONAL, ntII);
        Optional<NetworkTopology> oNT;
        try {
            oNT = lfONT.get();
        } catch (InterruptedException | ExecutionException ex) {
            return false;
        }
        if (oNT != null && oNT.isPresent()) {
            NetworkTopology networkTopo = oNT.get();
            for (Topology t : networkTopo.getTopology()) {
                for (Link l : t.getLink()) {
                    if (tpId.equals(l.getDestination().getDestTp())
                            || tpId.equals(l.getSource().getSourceTp())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void writeHosttoMDSAL(HostNode topologyHost) {

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey topologyNodeKey
                = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey(//
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId("host:" + topologyHost.getId().getValue()));

//        NodeKey normalNodeKey = new NodeKey(new NodeId((new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri(topologyNodeKey.getNodeId()))));


//        Node normalNode = new NodeBuilder().setKey(normalNodeKey).setId(normalNodeKey.getId()).build();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node topologyNode = //
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder().///
                setNodeId(topologyNodeKey.getNodeId()).addAugmentation(HostNode.class, topologyHost).build();


//        InstanceIdentifier<Node> nnIID
//                = InstanceIdentifier.builder(Nodes.class)
//                .child(Node.class, normalNodeKey).build();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> tnIID
                = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("flow:1")))
                //                .child(Topology.class) // THIS DOES NOT WORK!
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,//
                       topologyNodeKey).build();


//        log.trace("Writing inv  node to MD_SAL: " + nnIID.toString());
        log.trace("Writing host node to MD_SAL: " + tnIID.toString());

        ReadWriteTransaction writeTx = dataService.newReadWriteTransaction();
//        writeTx.put(LogicalDatastoreType.OPERATIONAL, nnIID, normalNode, true);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, tnIID, topologyNode, true);
        writeTx.submit();
    }
}
