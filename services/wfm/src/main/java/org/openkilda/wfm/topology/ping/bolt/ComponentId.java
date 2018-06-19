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

package org.openkilda.wfm.topology.ping.bolt;

public enum ComponentId {
    MONOTONIC_TICK("monotonic.tick"),

    FLOW_FETCHER("flow_fetcher"),
    PING_PRODUCER("ping_producer"),
    PING_ROUTER("ping_router"),

    BLACKLIST("blacklist"),

/*
    FLODDLIGHT_DECODER("floodlight.decoder"),
    FLOODLIGHT_ENCODER("floodlight.encoder"),

    FLOODLIGHT_IN("floodlight.kafka.in"),

    FLOW_SYNC_OUT("flow_sync.out"),
    FLOW_STATS_OUT("flow_stats.out"),
    FLOODLIGHT_OUT("floodlight.kafka.out")*/;

    private final String value;

    ComponentId(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
