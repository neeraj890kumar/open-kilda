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

    private PingReport.State currentState = null;
    private final HashMap<Long, PingObserver> observations = new HashMap<>();

    public FlowObserver(PingObserver.PingObserverBuilder pingStatusBuilder) {
        this.pingStatusBuilder = pingStatusBuilder;
    }

    /**
     * Update flow's state.
     */
    public void update(PingContext pingContext) {
        PingObserver pingObserver = observations.computeIfAbsent(
                pingContext.getCookie(), k -> pingStatusBuilder.build());

        long timestamp = pingContext.getTimestamp();
        if (pingContext.isError()) {
            pingObserver.markFailed(timestamp, pingContext.getError());
        } else {
            pingObserver.markOperational(timestamp);
        }
    }

    public void remove(long cookie) {
        observations.remove(cookie);
    }

    /**
     * Notify stored ping observers about end of time tick.
     */
    public PingReport.State timeTick(long timestamp) {
        PingReport.State flowState = PingReport.State.OPERATIONAL;
        for (Iterator<PingObserver> iterator = observations.values().iterator(); iterator.hasNext(); ) {
            final PingObserver value = iterator.next();

            PingObserver.State pingState = value.timeTick(timestamp);

            switch (pingState) {
                case GARBAGE:
                    iterator.remove();
                    continue;
                case FAIL:
                    flowState = PingReport.State.FAILED;
                    break;
                case UNKNOWN:
                case UNRELIABLE:
                    if (flowState == PingReport.State.OPERATIONAL) {
                        flowState = PingReport.State.UNRELIABLE;
                    }
                    break;

                default:
                    throw new IllegalArgumentException(String.format(
                            "Unsupported %s value %s", PingObserver.State.class.getName(), pingState));
            }
        }

        if (flowState == currentState) {
            flowState = null;
        } else {
            currentState = flowState;
        }

        return flowState;
    }

    /**
     * Return list of cookies for failed flows.
     */
    public List<Long> getFlowTreadsInState(PingReport.State reportState) {
        PingObserver.State pingState;
        switch (reportState) {
            case FAILED:
                pingState = PingObserver.State.FAIL;
                break;
            case OPERATIONAL:
                pingState = PingObserver.State.OPERATIONAL;
                break;
            case UNRELIABLE:
                pingState = PingObserver.State.UNRELIABLE;
                break;

            default:
                throw new IllegalArgumentException(String.format(
                        "Unsupported %s value %s", PingReport.State.class.getName(), reportState));
        }

        return observations.entrySet().stream()
                .filter(e -> e.getValue().getState() == pingState)
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean isGarbage() {
        return observations.size() == 0;
    }
}
