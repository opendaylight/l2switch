/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketChainGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketPayload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.Notification;

/**
 * An {@link AbstractDecoder} that operations on a previous packet chain output.
 *
 * @param <C> consumed notification type
 * @param <P> produced notification type
 */
public abstract non-sealed class SubsequentDecoder<
        C extends Notification<C> & DataObject & PacketChainGrp & PacketPayload,
        P extends Notification<P> & DataObject & PacketChainGrp & PacketPayload> extends AbstractDecoder<C, P> {
    protected SubsequentDecoder(final Class<C> consumedType, final Class<P> producedType) {
        super(consumedType, producedType);
    }

    @Override
    public final @Nullable P tryDecode(final @NonNull C input) {
        final var chainReceived = input.getPacketChain();
        return chainReceived == null || chainReceived.isEmpty() ? null : tryDecode(input, chainReceived);
    }

    /**
     * Try to decode a non-empty {@link PacketChain}.
     *
     * @param input the packet
     * @param chain the list of {@link PacketChain}, guaranteed to be non-empty
     * @return output of {@link #producedType()} or {@code null} if the packet cannot be decoded
     */
    protected abstract @Nullable P tryDecode(@NonNull C input, @NonNull List<PacketChain> chain);
}
