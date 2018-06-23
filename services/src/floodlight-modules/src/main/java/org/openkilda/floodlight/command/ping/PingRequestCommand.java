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
import org.openkilda.floodlight.model.PingData;
import org.openkilda.floodlight.service.PingService;
import org.openkilda.floodlight.service.batch.OfBatchService;
import org.openkilda.floodlight.service.batch.OfPendingMessage;
import org.openkilda.floodlight.switchmanager.OFInstallException;
import org.openkilda.messaging.floodlight.request.PingRequest;
import org.openkilda.messaging.model.Ping;
import org.openkilda.messaging.model.Ping.Errors;

import com.google.common.collect.ImmutableList;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.util.OFMessageUtils;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PingRequestCommand extends Abstract {
    private static Logger log = LoggerFactory.getLogger(PingRequestCommand.class);

    private final Ping ping;

    private final IOFSwitchService switchService;

    private final OfBatchService ioService;

    public PingRequestCommand(CommandContext context, PingRequest request) {
        super(context);

        ping = request.getPing();

        FloodlightModuleContext moduleContext = context.getModuleContext();
        switchService = moduleContext.getServiceImpl(IOFSwitchService.class);
        ioService = moduleContext.getServiceImpl(OfBatchService.class);
    }

    @Override
    public void execute() {
        try {
            validate();
            send();
        } catch (InsufficientCapabilitiesException e) {
            sendErrorResponse(ping.getPingId(), Errors.NOT_CAPABLE);
        }
    }

    @Override
    public void ioComplete(List<OfPendingMessage> payload, boolean isError) {
        if (!isError) {
            return;
        }

        sendErrorResponse(ping.getPingId(), Ping.Errors.WRITE_FAILURE);
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

    private void send() {
        String swId = ping.getSource().getSwitchDpId();
        IOFSwitch sw = lookupSwitch(swId);
        if (sw == null) {
            log.debug("Do not own ping's source switch {}", swId);
            return;
        }

        PingData data = PingData.of(ping);
        data.setSenderLatency(sw.getLatency().getValue());

        PingService pingService = getPingService();
        byte[] signedData = pingService.getSignature().sign(data);
        Ethernet packet = pingService.wrapData(ping, signedData);
        OFMessage message = makePacketOut(sw, packet.serialize());

        try {
            ioService.push(this, ImmutableList.of(new OfPendingMessage(sw.getId(), message)));
        } catch (OFInstallException e) {
            sendErrorResponse(ping.getPingId(), Ping.Errors.WRITE_FAILURE);
        }
    }

    private OFMessage makePacketOut(IOFSwitch sw, byte[] data) {
        OFFactory ofFactory = sw.getOFFactory();
        OFPacketOut.Builder pktOut = ofFactory.buildPacketOut();

        pktOut.setData(data);

        List<OFAction> actions = new ArrayList<>(2);
        actions.add(ofFactory.actions().buildOutput().setPort(OFPort.TABLE).build());
        pktOut.setActions(actions);

        OFMessageUtils.setInPort(pktOut, OFPort.of(ping.getSource().getPortId()));

        return pktOut.build();
    }

    private IOFSwitch lookupSwitch(String switchId) {
        DatapathId dpId = DatapathId.of(switchId);
        return switchService.getActiveSwitch(dpId);
    }
}
