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

package org.openkilda.messaging.floodlight.response;

import org.openkilda.messaging.info.InfoData;
import org.openkilda.messaging.model.Ping;
import org.openkilda.messaging.model.Ping.Errors;
import org.openkilda.messaging.model.PingMeters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class PingResponse extends InfoData {
    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("ping")
    private Ping ping;

    @JsonProperty("error")
    private Ping.Errors error;

    @JsonProperty("meters")
    private PingMeters meters;

    @JsonCreator
    public PingResponse(
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("ping") Ping ping,
            @JsonProperty("error") Errors error,
            @JsonProperty("meters") PingMeters meters) {
        this.timestamp = timestamp;
        this.ping = ping;
        this.error = error;
        this.meters = meters;
    }

    public PingResponse(Ping ping, Errors error, PingMeters meters) {
        this(System.currentTimeMillis(),
                ping, error, meters);
    }
}
