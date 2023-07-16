/*
 * Copyright 2023-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.pulsar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.ConsumerCryptoFailureAction;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;

/**
 * Configuration properties used to specify Pulsar consumers.
 *
 * @author Chris Bono
 * @since 3.2.0
 */
public class ConsumerConfigProperties {

	/**
	 * Comma-separated list of topics the consumer subscribes to.
	 */
	private Set<String> topics;

	/**
	 * Pattern for topics the consumer subscribes to.
	 */
	private Pattern topicsPattern;

	/**
	 * Subscription name for the consumer.
	 */
	private String subscriptionName;

	/**
	 * Subscription type to be used when subscribing to a topic.
	 */
	private SubscriptionType subscriptionType = SubscriptionType.Exclusive;

	/**
	 * Map of properties to add to the subscription.
	 */
	private Map<String, String> subscriptionProperties = new HashMap<>();

	/**
	 * Subscription mode to be used when subscribing to the topic.
	 */
	private SubscriptionMode subscriptionMode = SubscriptionMode.Durable;

	/**
	 * Number of messages that can be accumulated before the consumer calls "receive".
	 */
	private Integer receiverQueueSize = 1000;

	/**
	 * Time to group acknowledgements before sending them to the broker.
	 */
	private Duration acknowledgementsGroupTime = Duration.ofMillis(100);

	/**
	 * Delay before re-delivering messages that have failed to be processed.
	 */
	private Duration negativeAckRedeliveryDelay = Duration.ofMinutes(1);

	/**
	 * Maximum number of messages that a consumer can be pushed at once from a broker
	 * across all partitions.
	 */
	private Integer maxTotalReceiverQueueSizeAcrossPartitions = 50000;

	/**
	 * Consumer name to identify a particular consumer from the topic stats.
	 */
	private String consumerName;

	/**
	 * Timeout for unacked messages to be redelivered.
	 */
	private Duration ackTimeout = Duration.ZERO;

	/**
	 * Precision for the ack timeout messages tracker.
	 */
	private Duration tickDuration = Duration.ofSeconds(1);

	/**
	 * Priority level for shared subscription consumers.
	 */
	private Integer priorityLevel = 0;

	/**
	 * Action the consumer will take in case of decryption failure.
	 */
	private ConsumerCryptoFailureAction cryptoFailureAction = ConsumerCryptoFailureAction.FAIL;

	/**
	 * Map of properties to add to the consumer.
	 */
	private SortedMap<String, String> properties = new TreeMap<>();

	/**
	 * Whether to read messages from the compacted topic rather than the full message
	 * backlog.
	 */
	private Boolean readCompacted = false;

	/**
	 * Position where to initialize a newly created subscription.
	 */
	private SubscriptionInitialPosition subscriptionInitialPosition = SubscriptionInitialPosition.Latest;

	/**
	 * Auto-discovery period for topics when topic pattern is used in minutes.
	 */
	private Integer patternAutoDiscoveryPeriod = 1;

	/**
	 * Determines which topics the consumer should be subscribed to when using pattern
	 * subscriptions.
	 */
	private RegexSubscriptionMode regexSubscriptionMode = RegexSubscriptionMode.PersistentOnly;

	/**
	 * Dead letter policy to use.
	 */
	@NestedConfigurationProperty
	private DeadLetterPolicy deadLetterPolicy;

	/**
	 * Whether to auto retry messages.
	 */
	private Boolean retryEnable = false;

	/**
	 * Whether the consumer auto-subscribes for partition increase. This is only for
	 * partitioned consumers.
	 */
	private Boolean autoUpdatePartitions = true;

	/**
	 * Interval of partitions discovery updates.
	 */
	private Duration autoUpdatePartitionsInterval = Duration.ofMinutes(1);

	/**
	 * Whether to replicate subscription state.
	 */
	private Boolean replicateSubscriptionState = false;

	/**
	 * Whether to include the given position of any reset operation like
	 * {@link org.apache.pulsar.client.api.Consumer#seek(long) or
	 * {@link ConsumerConfigProperties#seek(MessageId)}}.
	 */
	private Boolean resetIncludeHead = false;

	/**
	 * Whether the batch index acknowledgment is enabled.
	 */
	private Boolean batchIndexAckEnabled = false;

	/**
	 * Whether an acknowledgement receipt is enabled.
	 */
	private Boolean ackReceiptEnabled = false;

	/**
	 * Whether pooling of messages and the underlying data buffers is enabled.
	 */
	private Boolean poolMessages = false;

	/**
	 * Whether to start the consumer in a paused state.
	 */
	private Boolean startPaused = false;

	/**
	 * Whether to automatically drop outstanding un-acked messages if the queue is full.
	 */
	private Boolean autoAckOldestChunkedMessageOnQueueFull = true;

	/**
	 * Maximum number of chunked messages to be kept in memory.
	 */
	private Integer maxPendingChunkedMessage = 10;

	/**
	 * Time to expire incomplete chunks if the consumer won't be able to receive all
	 * chunks before.
	 */
	private Duration expireTimeOfIncompleteChunkedMessage = Duration.ofMinutes(1);

	public Set<String> getTopics() {
		return this.topics;
	}

	public void setTopics(Set<String> topics) {
		this.topics = topics;
	}

	public Pattern getTopicsPattern() {
		return this.topicsPattern;
	}

	public void setTopicsPattern(Pattern topicsPattern) {
		this.topicsPattern = topicsPattern;
	}

	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	public Map<String, String> getSubscriptionProperties() {
		return this.subscriptionProperties;
	}

	public void setSubscriptionProperties(Map<String, String> subscriptionProperties) {
		this.subscriptionProperties = subscriptionProperties;
	}

	public SubscriptionMode getSubscriptionMode() {
		return this.subscriptionMode;
	}

	public void setSubscriptionMode(SubscriptionMode subscriptionMode) {
		this.subscriptionMode = subscriptionMode;
	}

	public SubscriptionType getSubscriptionType() {
		return this.subscriptionType;
	}

	public void setSubscriptionType(SubscriptionType subscriptionType) {
		this.subscriptionType = subscriptionType;
	}

	public Integer getReceiverQueueSize() {
		return this.receiverQueueSize;
	}

	public void setReceiverQueueSize(Integer receiverQueueSize) {
		this.receiverQueueSize = receiverQueueSize;
	}

	public Duration getAcknowledgementsGroupTime() {
		return this.acknowledgementsGroupTime;
	}

	public void setAcknowledgementsGroupTime(Duration acknowledgementsGroupTime) {
		this.acknowledgementsGroupTime = acknowledgementsGroupTime;
	}

	public Duration getNegativeAckRedeliveryDelay() {
		return this.negativeAckRedeliveryDelay;
	}

	public void setNegativeAckRedeliveryDelay(Duration negativeAckRedeliveryDelay) {
		this.negativeAckRedeliveryDelay = negativeAckRedeliveryDelay;
	}

	public Integer getMaxTotalReceiverQueueSizeAcrossPartitions() {
		return this.maxTotalReceiverQueueSizeAcrossPartitions;
	}

	public void setMaxTotalReceiverQueueSizeAcrossPartitions(Integer maxTotalReceiverQueueSizeAcrossPartitions) {
		this.maxTotalReceiverQueueSizeAcrossPartitions = maxTotalReceiverQueueSizeAcrossPartitions;
	}

	public String getConsumerName() {
		return this.consumerName;
	}

	public void setConsumerName(String consumerName) {
		this.consumerName = consumerName;
	}

	public Duration getAckTimeout() {
		return this.ackTimeout;
	}

	public void setAckTimeout(Duration ackTimeout) {
		this.ackTimeout = ackTimeout;
	}

	public Duration getTickDuration() {
		return this.tickDuration;
	}

	public void setTickDuration(Duration tickDuration) {
		this.tickDuration = tickDuration;
	}

	public Integer getPriorityLevel() {
		return this.priorityLevel;
	}

	public void setPriorityLevel(Integer priorityLevel) {
		this.priorityLevel = priorityLevel;
	}

	public ConsumerCryptoFailureAction getCryptoFailureAction() {
		return this.cryptoFailureAction;
	}

	public void setCryptoFailureAction(ConsumerCryptoFailureAction cryptoFailureAction) {
		this.cryptoFailureAction = cryptoFailureAction;
	}

	public SortedMap<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(SortedMap<String, String> properties) {
		this.properties = properties;
	}

	public Boolean getReadCompacted() {
		return this.readCompacted;
	}

	public void setReadCompacted(Boolean readCompacted) {
		this.readCompacted = readCompacted;
	}

	public SubscriptionInitialPosition getSubscriptionInitialPosition() {
		return this.subscriptionInitialPosition;
	}

	public void setSubscriptionInitialPosition(SubscriptionInitialPosition subscriptionInitialPosition) {
		this.subscriptionInitialPosition = subscriptionInitialPosition;
	}

	public Integer getPatternAutoDiscoveryPeriod() {
		return this.patternAutoDiscoveryPeriod;
	}

	public void setPatternAutoDiscoveryPeriod(Integer patternAutoDiscoveryPeriod) {
		this.patternAutoDiscoveryPeriod = patternAutoDiscoveryPeriod;
	}

	public RegexSubscriptionMode getRegexSubscriptionMode() {
		return this.regexSubscriptionMode;
	}

	public void setRegexSubscriptionMode(RegexSubscriptionMode regexSubscriptionMode) {
		this.regexSubscriptionMode = regexSubscriptionMode;
	}

	public DeadLetterPolicy getDeadLetterPolicy() {
		return this.deadLetterPolicy;
	}

	public void setDeadLetterPolicy(DeadLetterPolicy deadLetterPolicy) {
		this.deadLetterPolicy = deadLetterPolicy;
	}

	public Boolean getRetryEnable() {
		return this.retryEnable;
	}

	public void setRetryEnable(Boolean retryEnable) {
		this.retryEnable = retryEnable;
	}

	public Boolean getAutoUpdatePartitions() {
		return this.autoUpdatePartitions;
	}

	public void setAutoUpdatePartitions(Boolean autoUpdatePartitions) {
		this.autoUpdatePartitions = autoUpdatePartitions;
	}

	public Duration getAutoUpdatePartitionsInterval() {
		return this.autoUpdatePartitionsInterval;
	}

	public void setAutoUpdatePartitionsInterval(Duration autoUpdatePartitionsInterval) {
		this.autoUpdatePartitionsInterval = autoUpdatePartitionsInterval;
	}

	public Boolean getReplicateSubscriptionState() {
		return this.replicateSubscriptionState;
	}

	public void setReplicateSubscriptionState(Boolean replicateSubscriptionState) {
		this.replicateSubscriptionState = replicateSubscriptionState;
	}

	public Boolean getResetIncludeHead() {
		return this.resetIncludeHead;
	}

	public void setResetIncludeHead(Boolean resetIncludeHead) {
		this.resetIncludeHead = resetIncludeHead;
	}

	public Boolean getBatchIndexAckEnabled() {
		return this.batchIndexAckEnabled;
	}

	public void setBatchIndexAckEnabled(Boolean batchIndexAckEnabled) {
		this.batchIndexAckEnabled = batchIndexAckEnabled;
	}

	public Boolean getAckReceiptEnabled() {
		return this.ackReceiptEnabled;
	}

	public void setAckReceiptEnabled(Boolean ackReceiptEnabled) {
		this.ackReceiptEnabled = ackReceiptEnabled;
	}

	public Boolean getPoolMessages() {
		return this.poolMessages;
	}

	public void setPoolMessages(Boolean poolMessages) {
		this.poolMessages = poolMessages;
	}

	public Boolean getStartPaused() {
		return this.startPaused;
	}

	public void setStartPaused(Boolean startPaused) {
		this.startPaused = startPaused;
	}

	public Boolean getAutoAckOldestChunkedMessageOnQueueFull() {
		return this.autoAckOldestChunkedMessageOnQueueFull;
	}

	public void setAutoAckOldestChunkedMessageOnQueueFull(Boolean autoAckOldestChunkedMessageOnQueueFull) {
		this.autoAckOldestChunkedMessageOnQueueFull = autoAckOldestChunkedMessageOnQueueFull;
	}

	public Integer getMaxPendingChunkedMessage() {
		return this.maxPendingChunkedMessage;
	}

	public void setMaxPendingChunkedMessage(Integer maxPendingChunkedMessage) {
		this.maxPendingChunkedMessage = maxPendingChunkedMessage;
	}

	public Duration getExpireTimeOfIncompleteChunkedMessage() {
		return this.expireTimeOfIncompleteChunkedMessage;
	}

	public void setExpireTimeOfIncompleteChunkedMessage(Duration expireTimeOfIncompleteChunkedMessage) {
		this.expireTimeOfIncompleteChunkedMessage = expireTimeOfIncompleteChunkedMessage;
	}

	@SuppressWarnings("deprecation")
	public ConsumerBuilderCustomizer<?> toConsumerBuilderCustomizer() {
		return (consumerBuilder) -> {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getTopics).as(ArrayList::new).to(consumerBuilder::topics);
			map.from(this::getTopicsPattern).to(consumerBuilder::topicsPattern);
			map.from(this::getSubscriptionName).to(consumerBuilder::subscriptionName);
			map.from(this::getSubscriptionType).to(consumerBuilder::subscriptionType);
			map.from(this::getSubscriptionProperties).to(consumerBuilder::subscriptionProperties);
			map.from(this::getSubscriptionMode).to(consumerBuilder::subscriptionMode);
			map.from(this::getReceiverQueueSize).to(consumerBuilder::receiverQueueSize);
			map.from(this::getAcknowledgementsGroupTime)
				.as(Duration::toMillis)
				.to(consumerBuilder, (cb, val) -> cb.acknowledgmentGroupTime(val, TimeUnit.MILLISECONDS));
			map.from(this::getNegativeAckRedeliveryDelay)
				.as(Duration::toMillis)
				.to(consumerBuilder, (cb, val) -> cb.negativeAckRedeliveryDelay(val, TimeUnit.MILLISECONDS));
			map.from(this::getMaxTotalReceiverQueueSizeAcrossPartitions)
				.to(consumerBuilder::maxTotalReceiverQueueSizeAcrossPartitions);
			map.from(this::getConsumerName).to(consumerBuilder::consumerName);
			map.from(this::getAckTimeout)
				.as(Duration::toMillis)
				.to(consumerBuilder, (cb, val) -> cb.ackTimeout(val, TimeUnit.MILLISECONDS));
			map.from(this::getTickDuration)
				.as(Duration::toMillis)
				.to(consumerBuilder, (cb, val) -> cb.ackTimeoutTickTime(val, TimeUnit.MILLISECONDS));
			map.from(this::getPriorityLevel).to(consumerBuilder::priorityLevel);
			map.from(this::getCryptoFailureAction).to(consumerBuilder::cryptoFailureAction);
			map.from(this::getProperties).to(consumerBuilder::properties);
			map.from(this::getReadCompacted).to(consumerBuilder::readCompacted);
			map.from(this::getSubscriptionInitialPosition).to(consumerBuilder::subscriptionInitialPosition);
			map.from(this::getPatternAutoDiscoveryPeriod).to(consumerBuilder::patternAutoDiscoveryPeriod);
			map.from(this::getRegexSubscriptionMode).to(consumerBuilder::subscriptionTopicsMode);
			map.from(this::getDeadLetterPolicy).to(consumerBuilder::deadLetterPolicy);
			map.from(this::getRetryEnable).to(consumerBuilder::enableRetry);
			map.from(this::getAutoUpdatePartitions).to(consumerBuilder::autoUpdatePartitions);
			map.from(this::getAutoUpdatePartitionsInterval)
				.asInt(Duration::toMillis)
				.to(consumerBuilder, (cb, val) -> cb.autoUpdatePartitionsInterval(val, TimeUnit.MILLISECONDS));
			map.from(this::getReplicateSubscriptionState).to(consumerBuilder::replicateSubscriptionState);
			map.from(this::getResetIncludeHead).whenTrue().to((b) -> consumerBuilder.startMessageIdInclusive());
			map.from(this::getBatchIndexAckEnabled).to(consumerBuilder::enableBatchIndexAcknowledgment);
			map.from(this::getAckReceiptEnabled).to(consumerBuilder::isAckReceiptEnabled);
			map.from(this::getPoolMessages).to(consumerBuilder::poolMessages);
			map.from(this::getStartPaused).to(consumerBuilder::startPaused);
			map.from(this::getAutoAckOldestChunkedMessageOnQueueFull)
				.to(consumerBuilder::autoAckOldestChunkedMessageOnQueueFull);
			map.from(this::getMaxPendingChunkedMessage).to(consumerBuilder::maxPendingChunkedMessage);
			map.from(this::getExpireTimeOfIncompleteChunkedMessage)
				.as(Duration::toMillis)
				.to(consumerBuilder, (cb, val) -> cb.expireTimeOfIncompleteChunkedMessage(val, TimeUnit.MILLISECONDS));
		};
	}

}
