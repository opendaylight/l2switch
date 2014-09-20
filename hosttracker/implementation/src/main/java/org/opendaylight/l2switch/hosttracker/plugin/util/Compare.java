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
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsBuilder;

public class Compare {

    /**
     * Compare two addresses. This method is different than equals since it only
     * compares MAC, VLAN and IP Address.
     *
     * @param addr1 first Address to compare.
     * @param addr2 second Address to compare.
     * @return true if both have the same MAC, VLAN and IP Address, false
     * otherwise.
     */
    public static boolean Addresses(Addresses addr1, Addresses addr2) {
        return ((addr1.getMac() == null && addr2.getMac() == null)
                || (addr1.getMac() != null && addr1.getMac().equals(addr2.getMac())))
                && ((addr1.getVlan() == null && addr2.getVlan() == null)
                || (addr1.getVlan() != null && addr1.getVlan().equals(addr2.getVlan())))
                && ((addr1.getIp() == null && addr2.getIp() == null)
                || (addr1.getIp() != null && addr1.getIp().equals(addr2.getIp())));
    }

    /**
     * Compares two AttachmentPointsBuilder. This method is different than
     * equals since it only compares the TpId of both AttachmentPointsBuilder.
     *
     * @param atp1 first AttachmentPointsBuilder to compare.
     * @param atp2 second AttachmentPointsBuilder to compare.
     * @return true if both have the same TpId, false otherwise.
     */
    public static boolean AttachmentPointsBuilder(AttachmentPointsBuilder atp1, AttachmentPointsBuilder atp2) {
        return (atp1.getTpId() == null && atp2.getTpId() == null)
                || (atp1.getTpId() != null && atp1.getTpId().equals(atp2.getTpId()));
    }
}
