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
import org.openkilda.wfm.error.PipelineException;
import org.openkilda.wfm.topology.ping.model.PingContext;

import org.apache.storm.tuple.Tuple;

public abstract class ResultManager extends Abstract {
    @Override
    protected void handleInput(Tuple input) throws AbstractException {
        handle(input, pullPingContext(input));
    }

    protected void handle(Tuple input, PingContext pingContext) throws AbstractException {
        if (pingContext.isError()) {
            handleError(input, pingContext);
        } else {
            handleSuccess(input, pingContext);
        }
    }

    protected void handleSuccess(Tuple input, PingContext pingContext) throws PipelineException {}

    protected void handleError(Tuple input, PingContext pingContext) throws PipelineException {}
}
