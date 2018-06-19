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

package org.openkilda.wfm.topology.ping;

import org.openkilda.pce.provider.PathComputerAuth;
import org.openkilda.wfm.LaunchEnvironment;
import org.openkilda.wfm.config.Neo4jConfig;
import org.openkilda.wfm.error.ConfigurationException;
import org.openkilda.wfm.error.NameCollisionException;
import org.openkilda.wfm.topology.AbstractTopology;
import org.openkilda.wfm.topology.ping.bolt.Blacklist;
import org.openkilda.wfm.topology.ping.bolt.FlowFetcher;
import org.openkilda.wfm.topology.ping.bolt.MonotonicTick;
import org.openkilda.wfm.topology.ping.bolt.PingProducer;
import org.openkilda.wfm.topology.ping.bolt.PingRouter;
import org.openkilda.wfm.topology.ping.bolt.TimeoutManager;

import org.apache.storm.generated.StormTopology;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;

public class PingTopology extends AbstractTopology<PingTopologyConfig> {
    protected PingTopology(LaunchEnvironment env) throws ConfigurationException {
        super(env, PingTopologyConfig.class);
    }

    @Override
    public StormTopology createTopology() throws NameCollisionException {
        TopologyBuilder topology = new TopologyBuilder();

        // TODO: cancel error reporting for removed flows

        monotonicTick(topology);
        flowFetcher(topology);
        pingProducer(topology);
        pingRouter(topology);
        blacklist(topology);
        timeoutManager(topology);

//        attachFlowSync(topology);
//        attachFloodlightInput(topology);
//
//        attachPingTick(topology);
//        monotonicTick(topology);
//
//        attachFlowSyncDecoder(topology);
//        topology.setBolt(FloodlightDecoder.BOLT_ID, new FloodlightDecoder());
//        attachFlowUpdateObserver(topology);
//        attachFlowKeeper(topology);
//        topology.setBolt(RequestProducer.BOLT_ID, new RequestProducer());
//        topology.setBolt(ResponseConsumer.BOLT_ID, new ResponseConsumer());
//        topology.setBolt(FloodlightEncoder.BOLT_ID, new FloodlightEncoder());

        return topology.createTopology();
    }

//    private void attachFloodlightInput(TopologyBuilder topology) {
//        String spoutId = ComponentId.FLOODLIGHT_IN.toString();
//        KafkaSpout<String, String> spout = createKafkaSpout(config.getKafkaSpeakerTopic(), spoutId);
//        topology.setSpout(spoutId, spout);
//    }

    private void monotonicTick(TopologyBuilder topology) {
        topology.setBolt(MonotonicTick.BOLT_ID, new MonotonicTick(topologyConfig.getPingInterval()));
    }

    private void flowFetcher(TopologyBuilder topology) {
        Neo4jConfig neo4jConfig = configurationProvider.getConfiguration(Neo4jConfig.class);
        PathComputerAuth auth = new PathComputerAuth(neo4jConfig.getHost(),
                neo4jConfig.getLogin(), neo4jConfig.getPassword());

        FlowFetcher bolt = new FlowFetcher(auth);
        topology.setBolt(FlowFetcher.BOLT_ID, bolt)
                .shuffleGrouping(MonotonicTick.BOLT_ID, MonotonicTick.STREAM_PING_ID);
    }

    private void pingProducer(TopologyBuilder topology) {
        PingProducer bolt = new PingProducer();
        topology.setBolt(PingProducer.BOLT_ID, bolt)
                .fieldsGrouping(FlowFetcher.BOLT_ID, new Fields(FlowFetcher.FIELD_ID_FLOW_ID));
    }

    private void pingRouter(TopologyBuilder topology) {
        PingRouter bolt = new PingRouter();
        topology.setBolt(PingRouter.BOLT_ID, bolt)
                .shuffleGrouping(PingProducer.BOLT_ID)
                .shuffleGrouping(Blacklist.BOLT_ID);
    }

    private void blacklist(TopologyBuilder topology) {
        Blacklist bolt = new Blacklist();
        topology.setBolt(Blacklist.BOLT_ID, bolt)
                .fieldsGrouping(PingRouter.BOLT_ID, new Fields(PingRouter.FIELD_ID_PING_MATCH));
    }

    private void timeoutManager(TopologyBuilder topology) {
        TimeoutManager bolt = new TimeoutManager();
        topology.setBolt(TimeoutManager.BOLT_ID, bolt)
                .fieldsGrouping(PingRouter.BOLT_ID, new Fields(PingRouter.FIELD_ID_PING_ID));
    }

//    private void attachFlowSyncDecoder(TopologyBuilder topology) {
//        topology.setBolt(FlowSyncDecoder.BOLT_ID, new FlowSyncDecoder());
//    }
//
//    private void attachFlowUpdateObserver(TopologyBuilder topology) {
//        Auth pceAuth = config.getPathComputerAuth();
//        topology.setBolt(FlowSyncObserver.BOLT_ID, new FlowSyncObserver(pceAuth))
//                .allGrouping(MonotonicTick.BOLT_ID);
//    }

    /**
     * Topology entry point.
     */
    public static void main(String[] args) {
        try {
            LaunchEnvironment env = new LaunchEnvironment(args);
            (new PingTopology(env)).setup();
        } catch (Exception e) {
            System.exit(handleLaunchException(e));
        }
    }
}
