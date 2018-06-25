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

package org.openkilda.wfm.topology.flow.model;

import org.openkilda.messaging.command.flow.FlowDirection;
import org.openkilda.messaging.command.flow.FlowPingRequest;
import org.openkilda.messaging.command.flow.UniFlowVerificationRequest;
import org.openkilda.messaging.info.flow.FlowPingResponse;
import org.openkilda.messaging.info.flow.FlowVerificationErrorCode;
import org.openkilda.messaging.info.flow.UniFlowPingResponse;
import org.openkilda.messaging.model.BidirectionalFlow;
import org.openkilda.messaging.model.Flow;
import org.openkilda.wfm.topology.flow.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class VerificationWaitRecord {
    private final Constants constants = Constants.instance;

    private final long createTime;
    private final String correlationId;
    private final FlowPingResponse.FlowVerificationResponseBuilder response;

    private final HashMap<UUID, PendingRecord> pendingRequests = new HashMap<>();

    public VerificationWaitRecord(FlowPingRequest request, BidirectionalFlow biFlow, String correlationId) {
        this.createTime = System.currentTimeMillis();
        this.correlationId = correlationId;

        this.response = FlowPingResponse.builder();
        this.response.flowId(biFlow.getFlowId());

        addPending(request, biFlow.getForward(), FlowDirection.FORWARD);
        addPending(request, biFlow.getReverse(), FlowDirection.REVERSE);
    }

    /**
     * Save response for one currently pending request.
     */
    public boolean consumeResponse(UniFlowPingResponse payload) {
        PendingRecord pending = pendingRequests.remove(payload.getPacketId());
        if (pending == null) {
            return false;
        }

        saveUniResponse(pending, payload);
        return true;
    }

    public FlowPingResponse produce() {
        return response.build();
    }

    /**
     * Mark remaining pending request as failed.
     */
    public void fillPendingWithError(FlowVerificationErrorCode errorCode) {
        UniFlowPingResponse errorResponse;
        for (UUID packetId : pendingRequests.keySet()) {
            PendingRecord pending = pendingRequests.get(packetId);

            errorResponse = new UniFlowPingResponse(pending.request, errorCode);
            saveUniResponse(pending, errorResponse);
        }
        pendingRequests.clear();
    }

    public boolean isFilled() {
        return pendingRequests.size() == 0;
    }

    /**
     * Check is record become obsolete.
     */
    public boolean isOutdated(long currentTime) {
        long outdatedLimit = currentTime - constants.getVerificationRequestTimeoutMillis();
        if (outdatedLimit < 0) {
            outdatedLimit = 0;
        }

        return createTime < outdatedLimit;
    }

    /**
     * List pending requests.
     */
    public List<UniFlowVerificationRequest> getPendingRequests() {
        return pendingRequests.values().stream()
                .map(pending -> pending.request)
                .collect(Collectors.toList());
    }

    public String getCorrelationId() {
        return correlationId;
    }

    private void addPending(FlowPingRequest request, Flow flow, FlowDirection direction) {
        UniFlowVerificationRequest payload = new UniFlowVerificationRequest(request, flow, direction);
        PendingRecord pending = new PendingRecord(direction, payload);
        pendingRequests.put(payload.getPacketId(), pending);
    }

    private void saveUniResponse(PendingRecord pending, UniFlowPingResponse payload) {
        switch (pending.direction) {
            case FORWARD:
                response.forward(payload);
                break;
            case REVERSE:
                response.reverse(payload);
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Unhandled flow direction value: %s", pending.direction));
        }
    }

    private static class PendingRecord {
        final FlowDirection direction;
        final UniFlowVerificationRequest request;

        PendingRecord(FlowDirection direction, UniFlowVerificationRequest request) {
            this.direction = direction;
            this.request = request;
        }
    }
}
