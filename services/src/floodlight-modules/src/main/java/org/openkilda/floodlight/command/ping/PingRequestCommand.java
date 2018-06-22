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

package org.openkilda.floodlight.command.ping;

import org.openkilda.floodlight.command.CommandContext;
import org.openkilda.floodlight.error.InsufficientCapabilitiesException;
import org.openkilda.messaging.floodlight.request.PingRequest;
import org.openkilda.messaging.model.Ping;
import org.openkilda.messaging.model.Ping.Errors;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingRequestCommand extends Abstract {
    private static Logger log = LoggerFactory.getLogger(PingRequestCommand.class);

    private final Ping ping;

    private final IOFSwitchService switchService;

    public PingRequestCommand(CommandContext context, PingRequest request) {
        super(context, request.getPingId());

        ping = request.getPing();
        switchService = context.getModuleContext().getServiceImpl(IOFSwitchService.class);
    }

    @Override
    public void execute() {
        try {
            validate();
        } catch (InsufficientCapabilitiesException e) {
            sendErrorResponse(Errors.NOT_CAPABLE);
        }
    }

    private void validate() throws InsufficientCapabilitiesException {
        final String destId = ping.getDest().getSwitchDpId();
        IOFSwitch destSw = lookupSwitch(destId);
        if (destSw == null) {
            log.debug("Do not own ping\'s destination switch {}", destId);
            return;
        }

        if (0 < OFVersion.OF_13.compareTo(destSw.getOFFactory().getVersion())) {
            throw new InsufficientCapabilitiesException(String.format(
                    "Switch %s is not able to catch PING package", destId));
        }
    }

    private IOFSwitch lookupSwitch(String switchId) {
        DatapathId dpId = DatapathId.of(switchId);
        return switchService.getActiveSwitch(dpId);
    }
}
