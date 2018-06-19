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

import org.openkilda.wfm.CommandContext;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.ping.PingContext;

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

public class PingRouter extends AbstractBolt {
    public static final String BOLT_ID = ComponentId.PING_ROUTER.toString();

    public static final String FIELD_ID_PING_MATCH = "ping.match";

    public static final Fields STREAM_BLACKLIST_FIELDS = new Fields(
            FIELD_ID_PING_MATCH, FIELD_ID_PING, FIELD_ID_CONTEXT);
    public static final String STREAM_BLACKLIST_ID = "blacklist";

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        String component = input.getSourceComponent();
        if (PingProducer.BOLT_ID.equals(component)) {
            routePingProducer(input);
        // TODO
        } else {
            unhandledInput(input);
        }
    }

    private void routePingProducer(Tuple input) throws PipelineException {
        PingContext pingContext = getPingContext(input);
        CommandContext commandContext = getContext(input);

        Values payload = new Values(pingContext.getPing(), pingContext, commandContext);
        getOutput().emit(STREAM_BLACKLIST_ID, input, payload);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declareStream(STREAM_BLACKLIST_ID, STREAM_BLACKLIST_FIELDS);
        // TODO
    }
}
