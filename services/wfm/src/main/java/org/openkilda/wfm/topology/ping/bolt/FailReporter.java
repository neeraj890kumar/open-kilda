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

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

import java.util.concurrent.TimeUnit;

public class FailReporter extends Abstract {
    public static final String BOLT_ID = ComponentId.FAIL_REPORTER.toString();

    public static final String FIELD_ID_FLOW_SYNC = "flow_sync";

    public static final Fields STREAM_FIELDS = new Fields(FIELD_ID_FLOW_SYNC, FIELD_ID_CONTEXT);

    private final long failDelay;
    private final long failReset;

    public FailReporter(int failDelay, int failReset) {
        this.failDelay = TimeUnit.SECONDS.toMillis(failDelay);
        this.failReset = TimeUnit.SECONDS.toMillis(failReset);
    }

    @Override
    protected void handleInput(Tuple input) throws AbstractException {

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputManager) {
        outputManager.declare(STREAM_FIELDS);
    }
}
