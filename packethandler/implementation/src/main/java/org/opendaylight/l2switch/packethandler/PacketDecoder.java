/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.Notification;

/**
 * Abstract base class for decoding a step in the packet chain.
 */
public abstract class PacketDecoder<C extends Notification<C> & DataObject, P extends Notification<P> & DataObject> {
    private final @NonNull Class<C> consumedType;
    private final @NonNull Class<P> producedType;

    protected PacketDecoder(final Class<C> consumedType, final Class<P> producedType) {
        this.consumedType = requireNonNull(consumedType);
        this.producedType = requireNonNull(producedType);
    }

    final @NonNull Class<C> consumedType() {
        return consumedType;
    }

    final @NonNull Class<P> producedType() {
        return producedType;
    }

    protected abstract boolean canDecode(@NonNull C packetReceived);

    protected abstract @Nullable P decode(@NonNull C packetReceived);

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
            .add("consumes", consumedType.getName())
            .add("produces", producedType.getName())
            .toString();
    }
}
