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

import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.topology.ping.model.FlowObserver;
import org.openkilda.wfm.topology.ping.model.FlowObserversPool;

import lombok.extern.slf4j.Slf4j;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FailReporter extends Abstract {
    public static final String BOLT_ID = ComponentId.FAIL_REPORTER.toString();

    public static final String FIELD_ID_FLOW_SYNC = "flow_sync";

    public static final Fields STREAM_FIELDS = new Fields(FIELD_ID_FLOW_SYNC, FIELD_ID_CONTEXT);

    private final long failDelay;
    private final long failReset;
    private FlowObserversPool cache;

    public FailReporter(int failDelay, int failReset) {
        this.failDelay = TimeUnit.SECONDS.toMillis(failDelay);
        this.failReset = TimeUnit.SECONDS.toMillis(failReset);
    }

    @Override
    protected void init() {
        super.init();

        FlowObserver.FlowObserverBuilder builder = FlowObserver.builder()
                .failDelay(failDelay)
                .failReset(failReset);
        cache = new FlowObserversPool(builder);
    }

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        String component = input.getSourceComponent();

        if (MonotonicTick.BOLT_ID.equals(component)) {
            handleTick(input);
        // TODO
        } else {
            unhandledInput(input);
        }
    }

    private void handleTick(Tuple input) {
        final long now = input.getLongByField(MonotonicTick.FIELD_ID_TIME_MILLIS);
        ArrayList<String> failFlows = new ArrayList<>();

        for (FlowObserversPool.Entry entry : cache.getAll()) {
            FlowObserver flowObserver = entry.flowObserver;

            flowObserver.timeTick(now);
            if (flowObserver.isFail()) {
                failFlows.add(entry.flowId);
            }
        }

        for (String flowId : failFlows) {
            report(input, flowId, now);
        }
    }

    private void report(Tuple intput, String flowId, long now) {
        for (FlowObserver observer : cache.get(flowId)) {
            if (observer.isFail()) {
                observer.markReported(now);
            }
        }

        log.info("Flow {} is marked as FAILED due to ping check results", flowId);
        // TODO make sync record
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
