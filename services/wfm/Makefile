# WFM Makefile

TOPOLOGY := wfm flow stats cache islstats opentsdb portstate nbworker

ENTER_wfm := org.openkilda.wfm.topology.event.OFEventWFMTopology
ENTER_flow := org.openkilda.wfm.topology.flow.FlowTopology
ENTER_stats := org.openkilda.wfm.topology.stats.StatsTopology
ENTER_cache := org.openkilda.wfm.topology.cache.CacheTopology
ENTER_islstats := org.openkilda.wfm.topology.islstats.IslStatsTopology
ENTER_opentsdb := org.openkilda.wfm.topology.opentsdb.OpenTSDBTopology
ENTER_portstate := org.openkilda.wfm.topology.portstate.PortStateTopology
ENTER_nbworker := org.openkilda.wfm.topology.nbworker.NbWorkerTopology

TOPOLOGY_JAR := target/WorkflowManager-1.0-SNAPSHOT-jar-with-dependencies.jar

deploy_targets := $(addprefix deploy-,$(TOPOLOGY))
kill_targets := $(addprefix kill-,$(TOPOLOGY))

entry_point = $(ENTER_$(patsubst deploy-%,%,$@))
deploy_topology_name = $(patsubst deploy-%,%,$@)
kill_topology_name = $(patsubst kill-%,%,$@)

all-in-one-tested:
	mvn assembly:assembly

all-in-one:
	mvn assembly:assembly -DskipTests

deploy-all: $(deploy_targets)
kill-all: $(kill_targets)

$(deploy_targets):
	storm jar $(TOPOLOGY_JAR) $(entry_point) --name=$(deploy_topology_name) ${config}

$(kill_targets):
	storm kill $@ >/dev/null 2>&1

.PHONY: deploy-all kill-all
.PHONY: $(deploy_targets) $(kill_targets)
