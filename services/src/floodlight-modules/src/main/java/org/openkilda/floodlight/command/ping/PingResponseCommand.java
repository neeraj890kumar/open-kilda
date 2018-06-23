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

package org.openkilda.floodlight.command.ping;

import org.openkilda.floodlight.command.CommandContext;
import org.openkilda.floodlight.error.CorruptedNetworkDataException;
import org.openkilda.floodlight.model.PingData;
import org.openkilda.messaging.info.flow.UniFlowVerificationResponse;

public class PingResponseCommand extends Abstract {
    private final byte[] payload;

    public PingResponseCommand(CommandContext context, byte[] payload) {
        super(context);

        this.payload = payload;
    }

    @Override
    public void execute() {
        PingData data;
        try {
            data = PingData.of(token);

            if (!data.getDest().equals(sw.getId())) {
                throw new CorruptedNetworkDataException(String.format(
                        "Catch flow verification package on %s while target is %s", sw.getId(), data.getDest()));
            }
        } catch (CorruptedNetworkDataException e) {
            log.error(String.format("dpid:%s %s", sw.getId(), e));
            return false;
        }

        if (! verificationData.equals(payload)) {
            return false;
        }

        VerificationMeasures measures = payload.produceMeasurements(sw.getLatency().getValue());
        log.debug(
                "Receive flow VERIFICATION package - packetId: {}, latency: {}",
                payload.getPacketId(), measures.getNetworkLatency());
        UniFlowVerificationResponse response = new UniFlowVerificationResponse(getVerificationRequest(), measures);
        sendResponse(response);
    }
}
