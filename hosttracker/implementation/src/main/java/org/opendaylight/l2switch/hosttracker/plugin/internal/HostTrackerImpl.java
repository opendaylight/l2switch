package org.opendaylight.l2switch.hosttracker.plugin.internal;


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.mdhosttracker.rev140624.hosts.Host;
import org.opendaylight.yang.gen.v1.urn.opendaylight.mdhosttracker.rev140624.Hosts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.mdhosttracker.rev140624.hosts.HostBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.mdhosttracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author aanm
 * @version 0.0.1
 */
public class HostTrackerImpl implements DataChangeListener {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();

    private static final Logger log = LoggerFactory.getLogger(HostTrackerImpl.class);

    private DataBroker dataService;

    HostTrackerImpl(DataBroker dataService) {
        Preconditions.checkNotNull(dataService, "dataBrokerService should not be null.");
        this.dataService = dataService;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        log.trace("onDataChanged: " + change.toString());
        // TODO: we should really spawn a new thread to do this or get it from a threadpool
        //       to minimize how long we block the next notification
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
                                    writeHostToMDSAL(result, dataObject);
                                }
                            }

                            @Override
                            public void onFailure(Throwable arg0) {
                                // TODO Auto-generated method stub
                            }
                        });

                        //Host h = null;// = new Host();
                        //HostBuilder hb = new HostBuilder();
                        //hosts.add(hb);
                        // TODO: this should really be creating an "Entity" and passing to the logic of hosttracker_new
                        //       or something like it which will in turn, eventually manage a list of curated hosts
                        //
                        // The lists of curate hosts, will be published just like below, but after some pre-processing
                        // elsewhere.
                        //
                        // We may or may not want to keep a list of Entities in the MD-SAL data store, but I guessing
                        // not. Instead, we'll keep that as local variable(s) and just use them to do our processing.
                    }
                }
            }
        });
    }

    void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void writeHostToMDSAL(Optional<NodeConnector> result, DataObject dataObject) {
        Addresses addrs = (Addresses) dataObject;
        // NodeConnector nodeConnector = (NodeConnector) result.get();
        Host host = new HostBuilder().setAddresses(Arrays.asList(addrs)).setId(new HostId(addrs.getMac().getValue())).build();
        InstanceIdentifier<Host> hostId = InstanceIdentifier.builder(Hosts.class).child(Host.class, host.getKey()).build();
        ReadWriteTransaction writeTx = dataService.newReadWriteTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, hostId, host);
        writeTx.commit();
    }

    void registerAsDataChangeListener() {
        InstanceIdentifier<Addresses> addrCapableNodeConnectors = //
                InstanceIdentifier.builder(Nodes.class) //
                .child(Node.class).child(NodeConnector.class) //
                .augmentation(AddressCapableNodeConnector.class)//
                .child(Addresses.class).build();
        dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, addrCapableNodeConnectors, this, DataChangeScope.SUBTREE);
    }
}
