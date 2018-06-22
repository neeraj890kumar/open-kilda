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

import org.openkilda.messaging.model.PingReport;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.ping.model.FlowRef;
import org.openkilda.wfm.topology.ping.model.FlowStatus;
import org.openkilda.wfm.topology.ping.model.PingContext;
import org.openkilda.wfm.topology.ping.model.PingStatus;

import lombok.extern.slf4j.Slf4j;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class FailReporter extends Abstract {
    public static final String BOLT_ID = ComponentId.FAIL_REPORTER.toString();

    public static final String FIELD_ID_FLOW_SYNC = "flow_sync";

    public static final Fields STREAM_FIELDS = new Fields(FIELD_ID_FLOW_SYNC, FIELD_ID_CONTEXT);

    private final long failDelay;
    private final long failReset;
    private HashMap<String, FlowStatus> flowsStatusMap;
    private PingStatus.PingStatusBuilder pingStatusBuilder;

    public FailReporter(int failDelay, int failReset) {
        this.failDelay = TimeUnit.SECONDS.toMillis(failDelay);
        this.failReset = TimeUnit.SECONDS.toMillis(failReset);
    }

    @Override
    protected void init() {
        super.init();

        pingStatusBuilder = PingStatus.builder()
                .failDelay(failDelay)
                .failReset(failReset);
        flowsStatusMap = new HashMap<>();
    }

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        String component = input.getSourceComponent();

        if (MonotonicTick.BOLT_ID.equals(component)) {
            handleTick(input);
        } else if (FlowFetcher.BOLT_ID.equals(component)) {
            handleCacheExpiration(input);
        } else if (PeriodicResultManager.BOLT_ID.equals(component)) {
            handlePing(input);
        } else {
            unhandledInput(input);
        }
    }

    private void handleTick(Tuple input) throws PipelineException {
        final long now = input.getLongByField(MonotonicTick.FIELD_ID_TIME_MILLIS);

        for (Iterator<Entry<String, FlowStatus>> iterator = flowsStatusMap.entrySet().iterator();
                iterator.hasNext(); ) {

            Entry<String, FlowStatus> entry = iterator.next();
            FlowStatus flowStatus = entry.getValue();

            if (flowStatus.isGarbage()) {
                iterator.remove();
                continue;
            }

            PingReport.Status status = flowStatus.timeTick(now);
            if (status != null) {
                report(input, entry.getKey(), flowStatus, status);
            }
        }
    }

    private void handleCacheExpiration(Tuple input) throws PipelineException {
        FlowRef ref = pullFlowRef(input);
        FlowStatus status = flowsStatusMap.get(ref.flowId);
        if (status != null) {
            status.remove(ref.cookie);
        }
    }

    private void handlePing(Tuple input) throws PipelineException {
        PingContext pingContext = pullPingContext(input);
        if (pingContext.isPermanentError()) {
            log.warn("Do not include permanent ping error in report ({})", pingContext);
            return;
        }

        FlowStatus flowStatus = this.flowsStatusMap.computeIfAbsent(
                pingContext.getFlowId(), k -> new FlowStatus(pingStatusBuilder));
        flowStatus.update(pingContext);
    }

    private void report(Tuple input, String flowId, FlowStatus flowStatus, PingReport.Status status)
            throws PipelineException {
        String logMessage = String.format("{FLOW-PING} Flow %s become %s", flowId, status);
        if (status == PingReport.Status.FAILED) {
            String cookies = flowStatus.getFailedCookies().stream()
                    .map(cookie -> String.format("0x%016x", cookie))
                    .collect(Collectors.joining(", "));
            logMessage += String.format("(%s)", cookies);
        }

        log.info(logMessage);

        Values output = new Values(new PingReport(flowId, status), pullContext(input));
        getOutput().emit(input, output);
    }

    private FlowRef pullFlowRef(Tuple input) throws PipelineException {
        return pullValue(input, FlowFetcher.FIELD_FLOW_REF, FlowRef.class);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
