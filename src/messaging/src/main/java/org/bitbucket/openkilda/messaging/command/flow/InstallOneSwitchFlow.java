package org.bitbucket.openkilda.messaging.command.flow;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.bitbucket.openkilda.messaging.Utils.FLOW_ID;
import static org.bitbucket.openkilda.messaging.Utils.TRANSACTION_ID;

import org.bitbucket.openkilda.messaging.Utils;
import org.bitbucket.openkilda.messaging.payload.flow.OutputVlanType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * Class represents flow through one switch installation info.
 * Input and output vlan ids are optional, because flow could be untagged on ingoing or outgoing side.
 * Output action depends on flow input and output vlan presence.
 * Bandwidth and two meter ids are used for flow throughput limitation.
 */
@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "command",
        TRANSACTION_ID,
        FLOW_ID,
        "cookie",
        "switch_id",
        "input_port",
        "output_port",
        "input_vlan_id",
        "output_vlan_id",
        "output_vlan_type",
        "bandwidth",
        "src_meter_id",
        "dst_meter_id"})
public class InstallOneSwitchFlow extends BaseInstallFlow {
    /**
     * Serialization version number constant.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Allocated meter id.
     */
    @JsonProperty("src_meter_id")
    protected Long sourceMeterId;

    /**
     * Allocated meter id.
     */
    @JsonProperty("dst_meter_id")
    protected Long destinationMeterId;

    /**
     * Flow bandwidth value.
     */
    @JsonProperty("bandwidth")
    private Long bandwidth;

    /**
     * Output action on the vlan tag.
     */
    @JsonProperty("output_vlan_type")
    private OutputVlanType outputVlanType;

    /**
     * Optional input vlan id value.
     */
    @JsonProperty("input_vlan_id")
    private Integer inputVlanId;

    /**
     * Optional output vlan id value.
     */
    @JsonProperty("output_vlan_id")
    private Integer outputVlanId;

    /**
     * Instance constructor.
     *
     * @param transactionId      transaction id
     * @param id                 id of the flow
     * @param cookie             flow cookie
     * @param switchId           switch ID for flow installation
     * @param inputPort          input port of the flow
     * @param outputPort         output port of the flow
     * @param inputVlanId        input vlan id value
     * @param outputVlanId       output vlan id value
     * @param outputVlanType     output vlan tag action
     * @param bandwidth          flow bandwidth
     * @param sourceMeterId      source meter id
     * @param destinationMeterId destination meter id
     * @throws IllegalArgumentException if any of arguments is null
     */
    @JsonCreator
    public InstallOneSwitchFlow(@JsonProperty(TRANSACTION_ID) final Long transactionId,
                                @JsonProperty(FLOW_ID) final String id,
                                @JsonProperty("cookie") final Long cookie,
                                @JsonProperty("switch_id") final String switchId,
                                @JsonProperty("input_port") final Integer inputPort,
                                @JsonProperty("output_port") final Integer outputPort,
                                @JsonProperty("input_vlan_id") final Integer inputVlanId,
                                @JsonProperty("output_vlan_id") final Integer outputVlanId,
                                @JsonProperty("output_vlan_type") final OutputVlanType outputVlanType,
                                @JsonProperty("bandwidth") final Long bandwidth,
                                @JsonProperty("src_meter_id") final Long sourceMeterId,
                                @JsonProperty("dst_meter_id") final Long destinationMeterId) {
        super(transactionId, id, cookie, switchId, inputPort, outputPort);
        setInputVlanId(inputVlanId);
        setOutputVlanId(outputVlanId);
        setOutputVlanType(outputVlanType);
        setBandwidth(bandwidth);
        setSourceMeterId(sourceMeterId);
        setDestinationMeterId(destinationMeterId);
    }

    /**
     * Returns flow bandwidth value.
     *
     * @return flow bandwidth value
     */
    public Long getBandwidth() {
        return bandwidth;
    }

    /**
     * Sets flow bandwidth value.
     *
     * @param bandwidth bandwidth value
     */
    public void setBandwidth(final Long bandwidth) {
        if (bandwidth == null) {
            throw new IllegalArgumentException("need to set bandwidth");
        } else if (bandwidth < 0L) {
            throw new IllegalArgumentException("need to set non negative bandwidth");
        }
        this.bandwidth = bandwidth;
    }

    /**
     * Returns output action on the vlan tag.
     *
     * @return output action on the vlan tag
     */
    public OutputVlanType getOutputVlanType() {
        return outputVlanType;
    }

    /**
     * Sets output action on the vlan tag.
     *
     * @param outputVlanType action on the vlan tag
     */
    public void setOutputVlanType(final OutputVlanType outputVlanType) {
        if (outputVlanType == null) {
            throw new IllegalArgumentException("need to set output_vlan_type");
        } else if (!Utils.validateOutputVlanType(outputVlanId, outputVlanType)) {
            throw new IllegalArgumentException("need to set valid values for output_vlan_id and output_vlan_type");
        } else {
            this.outputVlanType = outputVlanType;
        }
    }

    /**
     * Returns input vlan id value.
     *
     * @return input vlan id value
     */
    public Integer getInputVlanId() {
        return inputVlanId;
    }

    /**
     * Sets input vlan id value.
     *
     * @param inputVlanId input vlan id value
     */
    public void setInputVlanId(final Integer inputVlanId) {
        if (inputVlanId == null) {
            this.inputVlanId = 0;
        } else if (Utils.validateVlanRange(inputVlanId)) {
            this.inputVlanId = inputVlanId;
        } else {
            throw new IllegalArgumentException("need to set valid value for input_vlan_id");
        }
    }

    /**
     * Returns output vlan id value.
     *
     * @return output vlan id value
     */
    public Integer getOutputVlanId() {
        return outputVlanId;
    }

    /**
     * Sets output vlan id value.
     *
     * @param outputVlanId output vlan id value
     */
    public void setOutputVlanId(final Integer outputVlanId) {
        if (outputVlanId == null) {
            this.outputVlanId = 0;
        } else if (Utils.validateVlanRange(outputVlanId)) {
            this.outputVlanId = outputVlanId;
        } else {
            throw new IllegalArgumentException("need to set valid value for output_vlan_id");
        }
    }

    /**
     * Returns source meter id for the flow.
     *
     * @return source meter id for the flow
     */
    public Long getSourceMeterId() {
        return sourceMeterId;
    }

    /**
     * Sets source meter id for the flow.
     *
     * @param sourceMeterId source meter id for the flow
     */
    public void setSourceMeterId(final Long sourceMeterId) {
        if (sourceMeterId == null) {
            throw new IllegalArgumentException("need to set src_meter_id");
        } else if (sourceMeterId < 0) {
            throw new IllegalArgumentException("need to set non negative src_meter_id");
        } else if (destinationMeterId != null && destinationMeterId != 0 && destinationMeterId.equals(sourceMeterId)) {
            throw new IllegalArgumentException("need to set different src_meter_id and dst_meter_id");
        }
        this.sourceMeterId = sourceMeterId;
    }

    /**
     * Returns destination meter id for the flow.
     *
     * @return destination meter id for the flow
     */
    public Long getDestinationMeterId() {
        return destinationMeterId;
    }

    /**
     * Sets destination meter id for the flow.
     *
     * @param destinationMeterId destination meter id for the flow
     */
    public void setDestinationMeterId(final Long destinationMeterId) {
        if (destinationMeterId == null) {
            throw new IllegalArgumentException("need to set dst_meter_id");
        } else if (destinationMeterId < 0L) {
            throw new IllegalArgumentException("need to set non negative dst_meter_id");
        } else if (sourceMeterId != null && sourceMeterId != 0 && sourceMeterId.equals(destinationMeterId)) {
            throw new IllegalArgumentException("need to set different src_meter_id and dst_meter_id");
        }
        this.destinationMeterId = destinationMeterId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toStringHelper(this)
                .add(TRANSACTION_ID, transactionId)
                .add(FLOW_ID, id)
                .add("cookie", cookie)
                .add("switch_id", switchId)
                .add("input_port", inputPort)
                .add("output_port", outputPort)
                .add("input_vlan_id", inputVlanId)
                .add("output_vlan_id", outputVlanId)
                .add("output_vlan_type", outputVlanType)
                .add("bandwidth", bandwidth)
                .add("src_meter_id", sourceMeterId)
                .add("dst_meter_id", destinationMeterId)
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        InstallOneSwitchFlow that = (InstallOneSwitchFlow) object;
        return Objects.equals(getTransactionId(), that.getTransactionId())
                && Objects.equals(getId(), that.getId())
                && Objects.equals(getCookie(), that.getCookie())
                && Objects.equals(getSwitchId(), that.getSwitchId())
                && Objects.equals(getInputPort(), that.getInputPort())
                && Objects.equals(getOutputPort(), that.getOutputPort())
                && Objects.equals(getInputVlanId(), that.getInputVlanId())
                && Objects.equals(getOutputVlanId(), that.getOutputVlanId())
                && Objects.equals(getOutputVlanType(), that.getOutputVlanType())
                && Objects.equals(getBandwidth(), that.getBandwidth())
                && Objects.equals(getSourceMeterId(), that.getSourceMeterId())
                && Objects.equals(getDestinationMeterId(), that.getDestinationMeterId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, id, cookie, switchId, inputPort, outputPort,
                inputVlanId, outputVlanId, outputVlanType, bandwidth, sourceMeterId, destinationMeterId);
    }
}