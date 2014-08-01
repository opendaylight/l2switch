package org.opendaylight.l2switch.hosttracker.plugin.internal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.Hosts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.hosts.Host;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.hosts.HostBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.hosts.HostKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTrackerImpl implements DataChangeListener {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();

    private static final Logger log = LoggerFactory.getLogger(HostTrackerImpl.class);

    private DataBroker dataService;

    private ConcurrentHashMap<HostKey, HostBuilder> hosts;

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

    private HostBuilder createHost(Addresses addrs, NodeConnector nodeConnector) {
        HostBuilder hostBuilder = new HostBuilder();

        if (addrs != null) {
            List setAddrs = new ArrayList();
            setAddrs.add(addrs);
            hostBuilder.setAddresses(setAddrs);
            hostBuilder.setId(new HostId(addrs.getMac().getValue()));
        } else {
            hostBuilder.setId(new HostId(Long.toString(this.id.getAndIncrement())));
        }
        hostBuilder.setKey(new HostKey(hostBuilder.getId()));

        if (nodeConnector != null) {
            InstanceIdentifier<NodeConnector> iinc//
                    = InstanceIdentifier.builder(Nodes.class)//
                    .child(Node.class)//
                    .child(NodeConnector.class, nodeConnector.getKey())//
                    .build();
            NodeConnectorRef ncr = new NodeConnectorRef(iinc);
            List setNodeConnectors = new ArrayList();
            setNodeConnectors.add(ncr);
            hostBuilder.setAttachmentPoint(setNodeConnectors);
        }
        return hostBuilder;
    }

    private void updateHost(HostBuilder oldHost, HostBuilder newHost) {
        synchronized (oldHost) {
            oldHost.getAddresses().removeAll(newHost.getAddresses());
            oldHost.getAddresses().addAll(newHost.getAddresses());
            oldHost.getAttachmentPoint().removeAll(newHost.getAttachmentPoint());
            oldHost.getAttachmentPoint().addAll(newHost.getAttachmentPoint());
        }
    }

    private void processHost(Optional<NodeConnector> result, DataObject dataObject) {
        Addresses addrs = (Addresses) dataObject;
        NodeConnector nodeConnector = (NodeConnector) result.get();
        if (!isNodeConnectorInternal(nodeConnector)) {
            HostBuilder host = createHost(addrs, nodeConnector);
            if (hosts.containsKey(host.getKey())) {
                updateHost(hosts.get(host.getKey()), host);
            } else {
                hosts.put(host.getKey(), host);
            }
            writeHosttoMDSAL(hosts.get(host.getKey()).build());
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

    private void writeHosttoMDSAL(Host host) {
        InstanceIdentifier<Host> hostId = InstanceIdentifier.builder(Hosts.class).child(Host.class, host.getKey()).build();
        ReadWriteTransaction writeTx = dataService.newReadWriteTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, hostId, host, true);
        writeTx.submit();
    }
}
