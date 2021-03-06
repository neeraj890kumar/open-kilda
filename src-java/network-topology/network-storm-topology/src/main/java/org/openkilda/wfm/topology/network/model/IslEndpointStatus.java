/* Copyright 2019 Telstra Open Source
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

package org.openkilda.wfm.topology.network.model;

import org.openkilda.model.IslDownReason;

import lombok.Data;
import lombok.Getter;

@Data
public class IslEndpointStatus {
    @Getter
    private Status status;

    @Getter
    private IslDownReason downReason;

    private boolean hasIslRules;

    public IslEndpointStatus(Status status) {
        this(status, null);
    }

    public IslEndpointStatus(Status status, IslDownReason downReason) {
        this.status = status;
        this.downReason = downReason;

    }

    public enum Status {
        UP, DOWN, MOVED
    }
}
