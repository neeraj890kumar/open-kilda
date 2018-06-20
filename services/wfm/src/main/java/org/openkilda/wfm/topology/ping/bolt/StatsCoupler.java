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

import org.openkilda.messaging.Utils;
import org.openkilda.messaging.model.FlowDirection;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.ping.model.ExpirableMap;
import org.openkilda.wfm.topology.ping.model.HalfFlowKey;
import org.openkilda.wfm.topology.ping.model.HalfFlowPingDescriptor;
import org.openkilda.wfm.topology.ping.model.PingContext;

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StatsCoupler extends Abstract {
    public static final String BOLT_ID = ComponentId.STATS_COUPLER.toString();

    public static final String FIELD_ID_FLOW_ID = Utils.FLOW_ID;
    public static final String FIELD_ID_PING_FORWARD = "ping.forward";
    public static final String FIELD_ID_PING_REVERSE = "ping.reverse";

    public static final Fields STREAM_FIELDS = new Fields(
            FIELD_ID_FLOW_ID, FIELD_ID_PING_FORWARD, FIELD_ID_PING_REVERSE, FIELD_ID_CONTEXT);

    private long interval;

    private ExpirableMap<HalfFlowKey, HalfFlowPingDescriptor> cache;
    private ArrayList<FlowDirection> orderHint;

    public StatsCoupler(int pingInterval) {
        interval = TimeUnit.SECONDS.toMillis(pingInterval);
    }

    @Override
    protected void init() {
        super.init();

        cache = new ExpirableMap<>();

        orderHint = new ArrayList<>(2);
        orderHint.add(FlowDirection.FORWARD);
        orderHint.add(FlowDirection.REVERSE);
    }

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        String component = input.getSourceComponent();
        if (MonotonicTick.BOLT_ID.equals(component)) {
            expire(input);
        } else if (PeriodicResultManager.BOLT_ID.equals(component)) {
            coupling(input);
        } else {
            unhandledInput(input);
        }
    }

    private void expire(Tuple input) {
        long now = input.getLongByField(MonotonicTick.FIELD_ID_TIME_MILLIS);
        cache.expire(now);
    }

    private void coupling(Tuple input) throws PipelineException {
        PingContext pingContext = pullPingContext(input);
        PingContext oppositePingContext = lookupOppositePing(pingContext);

        cacheUpdate(pingContext);

        ArrayList<Object> output = new ArrayList<>();
        output.add(pingContext.getFlowId());
        output.addAll(orderByDirection(pingContext, oppositePingContext));
        output.add(pullContext(input));

        getOutput().emit(input, output);
    }

    private PingContext lookupOppositePing(PingContext current) {
        HalfFlowKey key = new HalfFlowKey(current.getFlowId(), getOppositeDirection(current.getDirection()));
        HalfFlowPingDescriptor descriptor = cache.get(key);
        if (descriptor != null) {
            return descriptor.getPingContext();
        }
        return null;
    }

    private void cacheUpdate(PingContext current) {
        long expireAt = System.currentTimeMillis() + interval;
        HalfFlowPingDescriptor descriptor = new HalfFlowPingDescriptor(expireAt, current);
        cache.remove(descriptor.getExpirableKey());
        cache.add(descriptor);
    }

    private FlowDirection getOppositeDirection(FlowDirection current) {
        FlowDirection opposite;
        switch (current) {
            case FORWARD:
                opposite = FlowDirection.REVERSE;
                break;
            case REVERSE:
                opposite = FlowDirection.FORWARD;
                break;
            default:
                throw new IllegalArgumentException(String.format(
                        "Can\'t determine opposite direction for %s.%s", current.getClass().getName(), current));
        }
        return opposite;
    }

    private List<PingContext> orderByDirection(PingContext current, PingContext opposite) {
        PingContext[] result = new PingContext[] {null, null};
        result[orderHint.indexOf(current.getDirection())] = current;
        if (opposite != null) {
            result[orderHint.indexOf(opposite.getDirection())] = opposite;
        }

        return Arrays.asList(result);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
