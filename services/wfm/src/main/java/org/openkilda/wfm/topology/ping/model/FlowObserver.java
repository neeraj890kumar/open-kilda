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

package org.openkilda.wfm.topology.ping.model;

import org.openkilda.messaging.model.PingReport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class FlowObserver {
    private final PingObserver.PingObserverBuilder pingStatusBuilder;

    private PingReport.Status currentStatus = null;
    private final HashMap<Long, PingObserver> observations = new HashMap<>();

    public FlowObserver(PingObserver.PingObserverBuilder pingStatusBuilder) {
        this.pingStatusBuilder = pingStatusBuilder;
    }

    public void update(PingContext pingContext) {
        PingObserver pingObserver = observations.computeIfAbsent(pingContext.getCookie(), k -> pingStatusBuilder.build());

        long timestamp = pingContext.getTimestamp();
        if (pingContext.isError()) {
            pingObserver.markFailed(timestamp);
        } else {
            pingObserver.markOperational(timestamp);
        }
    }

    public void remove(long cookie) {
        observations.remove(cookie);
    }

    public PingReport.Status timeTick(long timestamp) {
        PingReport.Status status = PingReport.Status.OPERATIONAL;
        for (Iterator<PingObserver> iterator = observations.values().iterator(); iterator.hasNext(); ) {
            final PingObserver value = iterator.next();

            if (value.isGarbage()) {
                iterator.remove();
                continue;
            }

            value.timeTick(timestamp);
            if (value.isFail()) {
                status = PingReport.Status.FAILED;
            }
        }

        if (status == currentStatus) {
            status = null;
        } else {
            currentStatus = status;
        }

        return status;
    }

    public List<Long> getFailedCookies() {
        return observations.entrySet().stream()
                .filter(e -> e.getValue().isFail())
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean isGarbage() {
        return observations.size() == 0;
    }
}
