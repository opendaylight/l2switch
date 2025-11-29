/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketChainGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketPayload;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.Notification;

/**
 * Abstract base class for decoding a packet. This can either a {@link FirstDecoder} or a {@link SubsequentDecoder}.
 *
 * @param <C> consumed notification type
 * @param <P> produced notification type
 */
public abstract sealed class AbstractDecoder<
        C extends Notification<C> & DataObject,
        P extends Notification<P> & DataObject & PacketChainGrp & PacketPayload>
        permits FirstDecoder, SubsequentDecoder {
    private final @NonNull Class<C> consumedType;
    private final @NonNull Class<P> producedType;

    AbstractDecoder(final Class<C> consumedType, final Class<P> producedType) {
        this.consumedType = requireNonNull(consumedType);
        this.producedType = requireNonNull(producedType);
    }

    final @NonNull Class<C> consumedType() {
        return consumedType;
    }

    final @NonNull Class<P> producedType() {
        return producedType;
    }

    /**
     * Try to decodes the payload in given Packet further and returns a extension of Packet. e.g. ARP, IPV4, LLDP etc.
     *
     * @param input the input of {@link #consumedType()}
     * @return output of {@link #producedType()} or {@code null} if the packet cannot be decoded
     */
    protected abstract @Nullable P tryDecode(@NonNull C input);

    @Override
    public final String toString() {
        // Equivalent to MoreObjects.toStringHelper()
        return getClass().getSimpleName()
            + "{consumes=" + consumedType.getName() + ", produces=" + producedType.getName() + "}";
    }
}
