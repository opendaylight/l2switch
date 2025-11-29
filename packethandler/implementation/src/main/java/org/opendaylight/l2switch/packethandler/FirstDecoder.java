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
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.Notification;

/**
 * A {@link AbstractDecoder} operating on OpenFlow {@link PacketReceived} notification.
 *
 * @param <P> produced notification type
 */
public abstract non-sealed class FirstDecoder<
        P extends Notification<P> & DataObject & PacketChainGrp & PacketPayload>
        extends AbstractDecoder<PacketReceived, P> {
    protected FirstDecoder(final Class<P> producedType) {
        super(PacketReceived.class, producedType);
    }
}
