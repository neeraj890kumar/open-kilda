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

import org.openkilda.messaging.model.Ping;
import org.openkilda.wfm.error.AbstractException;
import org.openkilda.wfm.topology.ping.model.PingContext;

import lombok.extern.log4j.Log4j2;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

@Log4j2
public class Blacklist extends AbstractBolt {
    public static final String BOLT_ID = ComponentId.BLACKLIST.toString();

    public static final Fields STREAM_FIELDS = new Fields(FIELD_ID_PING, FIELD_ID_CONTEXT);

    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        PingContext pingContext = pullPingContext(input);
        if (isBlacklisted(pingContext.getPing())) {
            log.debug("{} canceled due to blacklist match", pingContext);
            return;
        }

        Values payload = new Values(pingContext, pullContext(input));
        getOutput().emit(input, payload);
    }

    private boolean isBlacklisted(Ping match) {
        // TODO
        return false;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
