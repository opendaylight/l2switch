/**
 * Copyright (c) 2015 Evan Zeller and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

interface HostTrackerOperation {
    void applyOperation(ReadWriteTransaction tx);
}
