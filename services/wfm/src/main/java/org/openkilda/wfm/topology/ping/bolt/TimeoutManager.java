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
import org.openkilda.messaging.command.CommandMessage;
import org.openkilda.messaging.floodlight.request.PingRequest;
import org.openkilda.messaging.model.Ping.Errors;
import org.openkilda.wfm.CommandContext;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.ping.model.PingContext;
import org.openkilda.wfm.topology.ping.model.TimeoutDescriptor;

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TimeoutManager extends AbstractBolt {
    public static final String BOLT_ID = ComponentId.TIMEOUT_MANAGER.toString();

    public static final String FIELD_ID_FLOW_ID = Utils.FLOW_ID;
    public static final String FIELD_ID_PAYLOAD = SpeakerEncoder.FIELD_ID_PAYLOAD;

    public static final Fields STREAM_REQUEST_FIELDS = new Fields(FIELD_ID_PAYLOAD, FIELD_ID_CONTEXT);
    public static final String STREAM_REQUEST_ID = "request";

    public static final Fields STREAM_RESPONSE_FIELDS = new Fields(); // TODO
    public static final String STREAM_RESPONSE_ID = "response";

    public static final Fields STREAM_TIMEOUT_FIELDS = new Fields(FIELD_ID_FLOW_ID, FIELD_ID_PING, FIELD_ID_CONTEXT);
    public static final String STREAM_TIMEOUT_ID = "timeout";

    private final long pingTimeout;

    private LinkedList<TimeoutDescriptor> pendingQueue;
    private HashMap<UUID, TimeoutDescriptor> pendingMap;

    public TimeoutManager(int pingTimeout) {
        this.pingTimeout = TimeUnit.SECONDS.toMillis(pingTimeout);
    }

    @Override
    protected void init() {
        super.init();

        pendingQueue = new LinkedList<>();
        pendingMap = new HashMap<>();
    }

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        String component = input.getSourceComponent();

        if (PingRouter.BOLT_ID.equals(component)) {
            handleRouter(input);
        } else if (MonotonicTick.BOLT_ID.equals(component)) {
            handleTimeTick(input);
        } else {
            unhandledInput(input);
        }
    }

    private void handleRouter(Tuple input) throws PipelineException {
        String stream = input.getSourceStreamId();

        if (PingRouter.STREAM_REQUEST_ID.equals(stream)) {
            handleRequest(input);
            // TODO
        } else {
            unhandledInput(input);
        }
    }

    private void handleTimeTick(Tuple input) {
        final long now = System.currentTimeMillis();
        while (! pendingQueue.isEmpty()) {
            TimeoutDescriptor descriptor = pendingQueue.getFirst();

            if (now < descriptor.getExpireAt()) {
                break;
            }

            pendingQueue.removeFirst();
            pendingMap.remove(descriptor.getPingContext().getPingId());
            if (! descriptor.isActive()) {
                continue;
            }

            emitTimeout(input, descriptor);
        }
    }

    private void handleRequest(Tuple input) throws PipelineException {
        PingContext pingContext = pullPingContext(input);
        CommandContext commandContext = pullContext(input);

        scheduleTimeout(pingContext, commandContext);
        emitRequest(input, pingContext, commandContext);
    }

    private void scheduleTimeout(PingContext pingContext, CommandContext commandContext) {
        long expireAt = System.currentTimeMillis() + pingTimeout;

        TimeoutDescriptor descriptor = new TimeoutDescriptor(expireAt, pingContext, commandContext);
        pendingQueue.addLast(descriptor);
        pendingMap.put(pingContext.getPingId(), descriptor);
    }

    private void emitRequest(Tuple input, PingContext pingContext, CommandContext commandContext) {
        final PingRequest request = new PingRequest(pingContext.getPing());
        Values payload = new Values(request, commandContext);
        getOutput().emit(STREAM_REQUEST_ID, input, payload);
    }

    private void emitTimeout(Tuple input, TimeoutDescriptor descriptor) {
        PingContext pingTimeout = descriptor.getPingContext().toBuilder().error(Errors.TIMEOUT).build();
        Values payload = new Values(pingTimeout.getFlowId(), pingTimeout, descriptor.getCommandContext());
        getOutput().emit(STREAM_TIMEOUT_ID, input, payload);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declareStream(STREAM_REQUEST_ID, STREAM_REQUEST_FIELDS);
        outputManager.declareStream(STREAM_RESPONSE_ID, STREAM_RESPONSE_FIELDS);
        outputManager.declareStream(STREAM_TIMEOUT_ID, STREAM_TIMEOUT_FIELDS);
    }
}
