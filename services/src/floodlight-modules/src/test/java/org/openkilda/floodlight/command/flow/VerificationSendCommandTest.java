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

package org.openkilda.floodlight.command.flow;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.openkilda.floodlight.service.batch.OfRequestResponse;
import org.openkilda.floodlight.switchmanager.OFInstallException;
import org.openkilda.floodlight.utils.DataSignature;
import org.openkilda.messaging.command.flow.UniFlowVerificationRequest;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.ver13.OFFactoryVer13;

import java.util.List;

public class VerificationSendCommandTest extends AbstractVerificationCommandTest {
    private DataSignature signature;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        signature = new DataSignature("secret");
    }

    @Test
    public void run() throws OFInstallException {
        expect(pingService.getSignature()).andReturn(signature);
        replay(pingService);

        OFFactory ofFactory = new OFFactoryVer13();
        expect(sourceSwitch.getOFFactory()).andReturn(ofFactory).anyTimes();
        replay(sourceSwitch, destSwitch);

        UniFlowVerificationRequest request = makeVerificationRequest();
        VerificationSendCommand subject = new VerificationSendCommand(context, request);

        Capture<List<OfRequestResponse>> capturePushPayload = newCapture(CaptureType.LAST);

        ioService.write(eq(subject), capture(capturePushPayload));
        expectLastCall().once();
        replay(ioService);

        subject.run();

        verify(pingService, switchService, sourceSwitch, destSwitch, ioService);

        List<OfRequestResponse> pushPayload = capturePushPayload.getValue();
        Assert.assertTrue("Send operation does not produce packet out message", 0 < pushPayload.size());
    }
}
