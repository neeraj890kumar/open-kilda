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

import org.openkilda.messaging.info.Datapoint;
import org.openkilda.messaging.model.PingMeters;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.error.WorkflowException;
import org.openkilda.wfm.topology.ping.model.Group;
import org.openkilda.wfm.topology.ping.model.OperationalStats;
import org.openkilda.wfm.topology.ping.model.PingContext;

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StatsProducer extends Abstract {
    public static final String BOLT_ID = ComponentId.STATS_PRODUCER.toString();

    public static final String FIELD_ID_STATS_RECORD = "stats";

    public static final Fields STREAM_FIELDS = new Fields(FIELD_ID_STATS_RECORD, FIELD_ID_CONTEXT);

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        Group group = pullPingGroup(input);

        validateGroup(input, group);
        String flowId = extractFlowId(input, group);
        produceStats(input, flowId, group);
    }

    private void validateGroup(Tuple input, Group group) throws WorkflowException {
        int expectRecords = 2;
        final int actualRecords = group.getRecords().size();
        if (actualRecords != expectRecords) {
            final String details = String.format("expect %d ping records, got %d", expectRecords, actualRecords);
            throw new WorkflowException(this, input, details);
        }
    }

    private String extractFlowId(Tuple input, Group group) throws WorkflowException {
        Set<String> idCollection = group.getFlowId();
        if (1 != idCollection.size()) {
            final String details = String.format(
                    "Expect ping data for exact 1 flow, got %d flowId", idCollection.size());
            throw new WorkflowException(this, input, details);
        }
        return idCollection.iterator().next();
    }

    private void produceStats(Tuple input, String flowId, Group group) throws PipelineException {
        HashMap<String, String> tags = new HashMap<>();
        tags.put("flowid", flowId);

        OperationalStats operational = new OperationalStats();
        for (PingContext pingContext : group.getRecords()) {
            operational.collect(pingContext);
            if (! pingContext.isError()) {
                produceMetersStats(input, new HashMap<>(tags), pingContext);
            }
        }

        produceOperationalStats(input, new HashMap<>(tags), operational);
    }

    private void produceMetersStats(Tuple input, Map<String, String> tags, PingContext pingContext)
            throws PipelineException {
        tags.put("direction", pingContext.getDirection().name().toLowerCase());

        PingMeters meters = pingContext.getMeters();
        Datapoint datapoint = new Datapoint(
                "pen.flow.latency", pingContext.getTimestamp(), tags, meters.getNetworkLatency());
        emit(input, datapoint);
    }

    private void produceOperationalStats(Tuple input, Map<String, String> tags, OperationalStats stats)
            throws PipelineException {
        Datapoint datapoint = new Datapoint("pen.flow.operational", stats.getTimestamp(), tags, stats.calculate());
        emit(input, datapoint);
    }

    private void emit(Tuple input, Datapoint datapoint) throws PipelineException {
        Values output = new Values(datapoint, pullContext(input));
        getOutput().emit(input, output);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
