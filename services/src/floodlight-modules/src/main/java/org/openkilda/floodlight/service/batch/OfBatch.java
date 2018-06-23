/* Copyright 2018 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.floodlight.service.batch;

import org.openkilda.floodlight.SwitchUtils;
import org.openkilda.floodlight.model.OfRequestResponse;
import org.openkilda.floodlight.switchmanager.OFInstallException;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Equals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OfBatch {
    private static final Logger log = LoggerFactory.getLogger(OfBatch.class);

    private final SwitchUtils switchUtils;

    private final HashMap<PendingKey, OfRequestResponse> pending;
    private final HashMap<PendingKey, OfRequestResponse> pendingBarrier;
    private final HashSet<DatapathId> affectedSwitches;

    private final List<OfRequestResponse> batch;
    private boolean errors = false;

    OfBatch(SwitchUtils switchUtils, List<OfRequestResponse> batch) {
        this.switchUtils = switchUtils;
        this.batch = batch;

        affectedSwitches = new HashSet<>();
        pending = makePendingMap(batch, affectedSwitches);

        List<OfRequestResponse> barriersBatch = makeBarriers(switchUtils, affectedSwitches);
        pendingBarrier = makePendingMap(barriersBatch, null);
    }

    void write() throws OFInstallException {
        HashMap<DatapathId, IOFSwitch> switchCache = new HashMap<>();
        writeBatch(batch, switchCache);
        writeBatch(pendingBarrier.values(), switchCache);

        log.debug("Write {}(+{}) messages", batch.size(), pendingBarrier.size());
    }

    void handleResponse(DatapathId dpId, OFMessage response) {
        PendingKey key = new PendingKey(dpId, response.getXid());
        OfRequestResponse entry;
        synchronized (pendingBarrier) {
            entry = pendingBarrier.remove(key);
        }
        if (entry != null) {
            log.debug("Have barrier response on {} ({})", dpId, response);
            return;
        }

        entry = pending.get(key);
        if (entry != null) {
            entry.setResponse(response);

            log.debug(
                    "Have response for some of payload messages (xId: {}, type: {})",
                    response.getXid(), response.getType());
            errors = OFType.ERROR == response.getType();
        }
    }

    private void writeBatch(Collection<OfRequestResponse> batch, Map<DatapathId, IOFSwitch> switchCache)
            throws OFInstallException {
        for (OfRequestResponse record : batch) {
            DatapathId dpId = record.getDpId();
            IOFSwitch sw = switchCache.computeIfAbsent(dpId, switchUtils::lookupSwitch);

            if (!sw.write(record.getRequest())) {
                throw new OFInstallException(dpId, record.getRequest());
            }
        }
    }

    boolean isComplete() {
        return pendingBarrier.size() == 0;
    }

    boolean isErrors() {
        return errors;
    }

    Set<DatapathId> getAffectedSwitches() {
        return affectedSwitches;
    }

    List<OfRequestResponse> getBatch() {
        return batch;
    }

    private static HashMap<PendingKey, OfRequestResponse> makePendingMap(
            List<OfRequestResponse> requests, Set<DatapathId> collectAffectedSwitches) {
        final HashSet<DatapathId> switches = new HashSet<>();
        final HashMap<PendingKey, OfRequestResponse> result = new HashMap<>();

        for (OfRequestResponse entry : requests) {
            PendingKey key = new PendingKey(entry.getDpId(), entry.getXid());
            result.put(key, entry);
            switches.add(entry.getDpId());
        }

        if (collectAffectedSwitches != null) {
            collectAffectedSwitches.addAll(switches);
        }

        return result;
    }

    private static List<OfRequestResponse> makeBarriers(SwitchUtils switchUtils, Set<DatapathId> switches) {
        final ArrayList<OfRequestResponse> result = new ArrayList<>();

        for (DatapathId dpId : switches) {
            IOFSwitch sw = switchUtils.lookupSwitch(dpId);
            result.add(new OfRequestResponse(dpId, sw.getOFFactory().barrierRequest()));
        }

        return result;
    }

    private static class PendingKey {
        DatapathId dpId;
        long xid;

        PendingKey(DatapathId dpId, long xid) {
            this.dpId = dpId;
            this.xid = xid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PendingKey that = (PendingKey) o;
            return new EqualsBuilder()
                    .append(xid, that.xid)
                    .append(dpId, that.dpId)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(dpId)
                    .append(xid)
                    .toHashCode();
        }
    }
}
