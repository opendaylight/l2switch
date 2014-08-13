/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All
 * rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPoints;

public class Compare {

    public static boolean Addresses(Addresses addr1, Addresses addr2) {
        return ((addr1.getMac() == null && addr2.getMac() == null)
                || (addr1.getMac() != null && addr1.getMac().equals(addr2.getMac())))
                && ((addr1.getVlan() == null && addr2.getVlan() == null)
                || (addr1.getVlan() != null && addr1.getVlan().equals(addr2.getVlan())))
                && ((addr1.getIp() == null && addr2.getIp() == null)
                || (addr1.getIp() != null && addr1.getIp().equals(addr2.getIp())));
    }

    public static boolean AttachmentPoints(AttachmentPoints atp1, AttachmentPoints atp2) {
        return (atp1.getTpId() == null && atp2.getTpId() == null)
                || (atp1.getTpId() != null && atp1.getTpId().equals(atp2.getTpId()));
    }
}
