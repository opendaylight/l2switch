/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketChainGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketPayload;
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
}
