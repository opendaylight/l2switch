/*
 * Copyright (c) 2015 Evan Zeller and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.openflowplugin.common.txchain.TransactionChainManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationProcessor implements AutoCloseable, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(OperationProcessor.class);
    private static final int NUM_RETRY_SUBMIT = 2;
    private static final int OPS_PER_CHAIN = 256;
    private static final int QUEUE_DEPTH = 512;
    private static final String HOSTTRACKER = "hosttracker";

    private final BlockingQueue<HostTrackerOperation> queue = new LinkedBlockingQueue<>(QUEUE_DEPTH);
    private final TransactionChainManager transactionChainManager;

    @SuppressFBWarnings("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR")
    OperationProcessor(final DataBroker dataBroker) {
        transactionChainManager = new TransactionChainManager(dataBroker, HOSTTRACKER);
        transactionChainManager.activateTransactionManager();
        transactionChainManager.initialSubmitWriteTransaction();
    }

    @Override
    public void run() {
        boolean done = false;
        while (!done) {
            try {
                HostTrackerOperation op = queue.take();

                int ops = 0;
                while (op != null && ops < OPS_PER_CHAIN) {
                    op.applyOperation(transactionChainManager);
                    ops += 1;
                    op = queue.poll();
                }

                LOG.debug("Processed {} operations, submitting transaction", ops);
                if (!transactionChainManager.submitTransaction()) {
                    clearQueue();
                }
            } catch (InterruptedException e) {
                done = true;
            }
        }
        clearQueue();
    }

    @Override
    public void close() {
        transactionChainManager.close();
    }

    public void enqueueOperation(HostTrackerOperation op) {
        try {
            queue.put(op);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void clearQueue() {
        while (!queue.isEmpty()) {
            queue.poll();
        }
    }

}
