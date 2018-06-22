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

public class FlowStatus {
    private final PingStatus.PingStatusBuilder pingStatusBuilder;

    private PingReport.Status currentStatus = null;
    private final HashMap<Long, PingStatus> pingReports = new HashMap<>();

    public FlowStatus(PingStatus.PingStatusBuilder pingStatusBuilder) {
        this.pingStatusBuilder = pingStatusBuilder;
    }

    public void update(PingContext pingContext) {
        PingStatus pingStatus = pingReports.computeIfAbsent(pingContext.getCookie(), k -> pingStatusBuilder.build());

        long timestamp = pingContext.getTimestamp();
        if (pingContext.isError()) {
            pingStatus.markFailed(timestamp);
        } else {
            pingStatus.markOperational(timestamp);
        }
    }

    public void remove(long cookie) {
        pingReports.remove(cookie);
    }

    public PingReport.Status timeTick(long timestamp) {
        PingReport.Status status = PingReport.Status.OPERATIONAL;
        for (Iterator<PingStatus> iterator = pingReports.values().iterator(); iterator.hasNext(); ) {
            final PingStatus value = iterator.next();

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
        return pingReports.entrySet().stream()
                .filter(e -> e.getValue().isFail())
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean isGarbage() {
        return pingReports.size() == 0;
    }
}
