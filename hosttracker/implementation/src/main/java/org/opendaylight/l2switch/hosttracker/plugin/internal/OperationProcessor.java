/*
 * Copyright (c) 2015 Evan Zeller and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.hosttracker.plugin.internal;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationProcessor implements AutoCloseable, Runnable, TransactionChainListener {
    private static final int NUM_RETRY_SUBMIT = 2;
    private static final int OPS_PER_CHAIN = 256;
    private static final int QUEUE_DEPTH = 512;

    private static final Logger LOG = LoggerFactory.getLogger(OperationProcessor.class);
    private final DataBroker dataBroker;
    private final BlockingQueue<HostTrackerOperation> queue;
    private final AtomicReference<TransactionChain> transactionChain = new AtomicReference<>();

    OperationProcessor(final DataBroker dataBroker) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.queue = new LinkedBlockingQueue<>(QUEUE_DEPTH);
        this.transactionChain.set(dataBroker.createTransactionChain(this));
    }


    public void onTransactionChainFailed(TransactionChain chain, Transaction transaction,
                                          Throwable cause) {
        chainFailure();
    }

    public void onTransactionChainSuccessful(TransactionChain chain) {
    }


    @Override
    public void run() {
        boolean done = false;
        while (!done) {
            try {
                HostTrackerOperation op = queue.take();
                final TransactionChain txChain = transactionChain.get();
                if (txChain == null) {
                    break;
                }

                ReadWriteTransaction tx = txChain.newReadWriteTransaction();
                //ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

                int ops = 0;
                while (op != null && ops < OPS_PER_CHAIN) {
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
    public void close() {
        final TransactionChain txChain = transactionChain.getAndSet(null);
        if (txChain != null) {
            txChain.close();
        }
    }

    private void chainFailure() {
        try {
            final TransactionChain prevChain = transactionChain.getAndSet(
                    dataBroker.createTransactionChain(this));
            if (prevChain != null) {
                prevChain.close();
            }
            clearQueue();
        } catch (IllegalStateException e) {
            LOG.warn("Failed to close chain", e);
        }
    }

    public void enqueueOperation(HostTrackerOperation op) {
        try {
            queue.put(op);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void submitTransaction(final ReadWriteTransaction tx, final int tries) {
        Futures.addCallback(tx.commit(), new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object obj) {
                LOG.trace("tx {} succeeded", tx.getIdentifier());
            }

            @Override
            public void onFailure(Throwable failure) {
                if (failure instanceof OptimisticLockFailedException) {
                    if (tries - 1 > 0) {
                        LOG.warn("tx {} failed, retrying", tx.getIdentifier());
                        // do retry
                        submitTransaction(tx, tries - 1);
                    } else {
                        LOG.warn("tx {} failed, out of retries", tx.getIdentifier());
                        // out of retries
                        chainFailure();
                    }
                } else {
                    // failed due to another type of
                    // TransactionCommitFailedException.
                    LOG.warn("tx {} failed: {}", tx.getIdentifier(), failure.getMessage());
                    chainFailure();
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void clearQueue() {
        while (!queue.isEmpty()) {
            queue.poll();
        }
    }

}
