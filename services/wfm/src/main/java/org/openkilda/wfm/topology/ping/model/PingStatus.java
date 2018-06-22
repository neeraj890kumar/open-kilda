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

import lombok.Builder;
import lombok.Data;

@Data
public class PingStatus {
    private final long failDelay;
    private final long failReset;
    private final long garbageDelay;

    private State state = State.UNKNOWN;
    private long lastStateTransitionAt = 0L;

    @Builder
    public PingStatus(long failDelay, long failReset) {
        this.failDelay = failDelay;
        this.failReset = failReset;
        this.garbageDelay = failReset;
    }

    public void markOperational(long timestamp) {
        dispatch(Event.STATE_OPERATIONAL, timestamp);
    }

    public void markFailed(long timestamp) {
        dispatch(Event.STATE_FAIL, timestamp);
    }

    public void timeTick(long timestamp) {
        dispatch(Event.TIME, timestamp);
    }

    public boolean isFail() {
        return state == State.FAIL;
    }

    public boolean isGarbage() {
        return state == State.GARBAGE;
    }

    private void dispatch(Event event, long timestamp) {
        switch (state) {
            case UNKNOWN:
            case GARBAGE:
                dispatchUnknown(event, timestamp);
                break;
            case OPERATIONAL:
                dispatchOperational(event, timestamp);
                break;
            case PRE_FAIL:
                dispatchPreFail(event, timestamp);
                break;
            case FAIL:
                dispatchFail(event, timestamp);
                break;

            default:
                throw new IllegalArgumentException(String.format("Unknown %s value %s", State.class.getName(), state));
        }
    }

    private void dispatchUnknown(Event event, long timestamp) {
        switch (event) {
            case TIME:
                if (lastStateTransitionAt + garbageDelay < timestamp) {
                    stateTransition(State.GARBAGE, timestamp);
                }
                break;
            case STATE_OPERATIONAL:
                stateTransition(State.OPERATIONAL, timestamp);
                break;
            case STATE_FAIL:
                stateTransition(State.PRE_FAIL, timestamp);
                break;

            default:
        }
    }

    private void dispatchOperational(Event event, long timestamp) {
        switch (event) {
            case STATE_FAIL:
                stateTransition(State.PRE_FAIL, timestamp);
                break;

            default:
        }
    }

    private void dispatchPreFail(Event event, long timestamp) {
        switch (event) {
            case TIME:
                if (lastStateTransitionAt + failDelay < timestamp) {
                    stateTransition(State.FAIL, timestamp);
                }
                break;
            case STATE_OPERATIONAL:
                stateTransition(State.OPERATIONAL, timestamp);
                break;

            default:
        }
    }

    private void dispatchFail(Event event, long timestamp) {
        switch (event) {
            case TIME:
                if (lastStateTransitionAt + failReset < timestamp) {
                    stateTransition(State.UNKNOWN, timestamp);
                }
                break;
            case STATE_OPERATIONAL:
                stateTransition(State.OPERATIONAL, timestamp);
                break;

            default:
        }
    }

    private void stateTransition(State target, long timetamp) {
        stateTransition(target);
        this.lastStateTransitionAt = timetamp;
    }

    private void stateTransition(State target) {
        this.state = target;
    }

    private enum Event {
        TIME,
        STATE_OPERATIONAL,
        STATE_FAIL,
        STATE_REPORTED
    }

    private enum State {
        UNKNOWN,
        OPERATIONAL,
        PRE_FAIL,
        FAIL,
        GARBAGE
    }
}
