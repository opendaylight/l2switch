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
 * A {@link PacketDecoder} that operations on a previous packet chain output.
 */
public abstract non-sealed class PacketChainDecoder<
        C extends Notification<C> & DataObject & PacketChainGrp & PacketPayload,
        P extends Notification<P> & DataObject & PacketChainGrp & PacketPayload> extends PacketDecoder<C, P> {
    protected PacketChainDecoder(final Class<C> consumedType, final Class<P> producedType) {
        super(consumedType, producedType);
    }
}
