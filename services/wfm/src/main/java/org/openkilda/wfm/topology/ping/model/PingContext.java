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

import org.openkilda.messaging.model.BidirectionalFlow;
import org.openkilda.messaging.model.FlowDirection;
import org.openkilda.messaging.model.Ping;
import org.openkilda.messaging.model.PingMeters;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class PingContext implements Serializable {
    public enum Kinds {
        PERIODIC,
        MANUAL
    }

    private Kinds kind;
    private UUID pingCoupleId;
    private BidirectionalFlow flow;
    private FlowDirection direction;

    private Ping ping;

    private Ping.Errors error;
    private PingMeters meters;

    public PingContext(Kinds kind, BidirectionalFlow flow) {
        this.kind = kind;
        this.flow = flow;
        this.pingCoupleId = UUID.randomUUID();
    }

    public String getFlowId() {
        return flow.getFlowId();
    }

    public UUID getPingId() {
        if (ping == null) {
            return null;
        }
        return ping.getPingId();
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isPermanentError() {
        if (! isError()) {
            return false;
        }

        boolean result;
        switch (error) {
            case NOT_CAPABLE:
                result = true;
                break;

            default:
                result = false;
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "<%s{flowId=%s, error=%s: %s}>", getClass().getCanonicalName(), getFlowId(), getError(), getPing());
    }
}
