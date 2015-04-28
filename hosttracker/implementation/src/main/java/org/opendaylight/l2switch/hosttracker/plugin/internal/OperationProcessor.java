/**
 * Copyright (c) 2015 Evan Zeller and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class OperationProcessor implements AutoCloseable, Runnable,
        TransactionChainListener {
    private static final int NUM_RETRY_SUBMIT = 2;
    private static final int OPS_PER_CHAIN = 256;
    private static final int QUEUE_DEPTH = 512;

    private static final Logger log = LoggerFactory
            .getLogger(OperationProcessor.class);
    private final DataBroker dataBroker;
    private final BlockingQueue<HostTrackerOperation> queue;
    private BindingTransactionChain transactionChain;

    OperationProcessor(final DataBroker dataBroker) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.queue = new LinkedBlockingQueue<HostTrackerOperation>(QUEUE_DEPTH);
        this.transactionChain = dataBroker.createTransactionChain(this);
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain,
            AsyncTransaction<?, ?> transaction, Throwable cause) {
        chainFailure();
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
    }

    @Override
    public void run() {
        boolean done = false;
        while (!done) {
            try {
                HostTrackerOperation op = queue.take();
                ReadWriteTransaction tx = transactionChain
                        .newReadWriteTransaction();

                int ops = 0;
                while ((op != null) && (ops < OPS_PER_CHAIN)) {
                    op.applyOperation(tx);
                    ops += 1;
                    op = queue.poll();
                }

                submitTransaction(tx, NUM_RETRY_SUBMIT);
            } catch (InterruptedException e) {
                done = true;
            }
        }
        clearQueue();
    }

    @Override
    public void close() throws Exception {
        if (transactionChain != null) {
            transactionChain.close();
        }
    }

    private void chainFailure() {
        try{
            transactionChain.close();
            transactionChain = dataBroker.createTransactionChain(this);
            clearQueue();
        }catch(IllegalStateException e){
            log.warn(e.getLocalizedMessage());
        }
    }

    public void enqueueOperation(HostTrackerOperation op){
        try {
            queue.put(op);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void submitTransaction(final ReadWriteTransaction tx, final int tries) {
        Futures.addCallback(tx.submit(), new FutureCallback<Object>() {
            public void onSuccess(Object o) {
                log.trace("tx {} succeeded", tx.getIdentifier());
            }

            public void onFailure(Throwable t) {
                if (t instanceof OptimisticLockFailedException) {
                    if ((tries - 1) > 0) {
                        log.warn("tx {} failed, retrying", tx.getIdentifier());
                        // do retry
                        submitTransaction(tx, tries - 1);
                    } else {
                        log.warn("tx {} failed, out of retries", tx.getIdentifier());
                        // out of retries
                        chainFailure();
                    }
                } else {
                    // failed due to another type of
                    // TransactionCommitFailedException.
                    log.warn("tx {} failed: {}", t.getMessage());
                    chainFailure();
                }
            }
        });
    }

    private void clearQueue() {
        while (!queue.isEmpty()) {
            queue.poll();
        }
    }

}
