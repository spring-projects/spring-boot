/*
 * Copyright 2012-2023 the original author or authors.
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

import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction;
import org.apache.pulsar.client.api.DeadLetterPolicy;
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

	private final Acknowledgement ack = new Acknowledgement();

	private final Chunking chunk = new Chunking();

	private final Subscription subscription = new Subscription();

	/**
	 * Comma-separated list of topics the consumer subscribes to.
	 */
	private Set<String> topics;

	/**
	 * Pattern for topics the consumer subscribes to.
	 */
	private Pattern topicsPattern;

	/**
	 * Number of messages that can be accumulated before the consumer calls "receive".
	 */
	private Integer receiverQueueSize = 1000;

	/**
	 * Maximum number of messages that a consumer can be pushed at once from a broker
	 * across all partitions.
	 */
	private Integer maxTotalReceiverQueueSizeAcrossPartitions = 50000;

	/**
	 * Consumer name to identify a particular consumer from the topic stats.
	 */
	private String name;

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
	 * Auto-discovery period for topics when topic pattern is used in minutes.
	 */
	private Integer patternAutoDiscoveryPeriod = 1;

	/**
	 * Dead letter policy to use.
	 */
	@NestedConfigurationProperty
	private DeadLetterPolicyConfig deadLetterPolicy = new DeadLetterPolicyConfig();

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
	 * Whether to include the given position of any reset operation (eg. the various seek
	 * APIs on the Pulsar consumer).
	 */
	private Boolean resetIncludeHead = false;

	/**
	 * Whether pooling of messages and the underlying data buffers is enabled.
	 */
	private Boolean poolMessages = false;

	/**
	 * Whether to start the consumer in a paused state.
	 */
	private Boolean startPaused = false;

	public Acknowledgement getAck() {
		return this.ack;
	}

	public Chunking getChunk() {
		return this.chunk;
	}

	public Subscription getSubscription() {
		return this.subscription;
	}

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

	public Integer getReceiverQueueSize() {
		return this.receiverQueueSize;
	}

	public void setReceiverQueueSize(Integer receiverQueueSize) {
		this.receiverQueueSize = receiverQueueSize;
	}

	public Integer getMaxTotalReceiverQueueSizeAcrossPartitions() {
		return this.maxTotalReceiverQueueSizeAcrossPartitions;
	}

	public void setMaxTotalReceiverQueueSizeAcrossPartitions(Integer maxTotalReceiverQueueSizeAcrossPartitions) {
		this.maxTotalReceiverQueueSizeAcrossPartitions = maxTotalReceiverQueueSizeAcrossPartitions;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Integer getPatternAutoDiscoveryPeriod() {
		return this.patternAutoDiscoveryPeriod;
	}

	public void setPatternAutoDiscoveryPeriod(Integer patternAutoDiscoveryPeriod) {
		this.patternAutoDiscoveryPeriod = patternAutoDiscoveryPeriod;
	}

	public DeadLetterPolicyConfig getDeadLetterPolicy() {
		return this.deadLetterPolicy;
	}

	public void setDeadLetterPolicy(DeadLetterPolicyConfig deadLetterPolicy) {
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

	public Boolean getResetIncludeHead() {
		return this.resetIncludeHead;
	}

	public void setResetIncludeHead(Boolean resetIncludeHead) {
		this.resetIncludeHead = resetIncludeHead;
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

	@SuppressWarnings("deprecation")
	public ConsumerBuilderCustomizer<?> toConsumerBuilderCustomizer() {
		return (consumerBuilder) -> {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getTopics).as(ArrayList::new).to(consumerBuilder::topics);
			map.from(this::getTopicsPattern).to(consumerBuilder::topicsPattern);
			map.from(this::getReceiverQueueSize).to(consumerBuilder::receiverQueueSize);
			map.from(this::getMaxTotalReceiverQueueSizeAcrossPartitions)
				.to(consumerBuilder::maxTotalReceiverQueueSizeAcrossPartitions);
			map.from(this::getName).to(consumerBuilder::consumerName);
			map.from(this::getPriorityLevel).to(consumerBuilder::priorityLevel);
			map.from(this::getCryptoFailureAction).to(consumerBuilder::cryptoFailureAction);
			map.from(this::getProperties).to(consumerBuilder::properties);
			map.from(this::getReadCompacted).to(consumerBuilder::readCompacted);
			map.from(this::getPatternAutoDiscoveryPeriod).to(consumerBuilder::patternAutoDiscoveryPeriod);
			map.from(this::getDeadLetterPolicy)
				.as(this::toPulsarDeadLetterPolicy)
				.to(consumerBuilder::deadLetterPolicy);
			map.from(this::getRetryEnable).to(consumerBuilder::enableRetry);
			map.from(this::getAutoUpdatePartitions).to(consumerBuilder::autoUpdatePartitions);
			map.from(this::getAutoUpdatePartitionsInterval)
				.asInt(Duration::toMillis)
				.to(consumerBuilder, (cb, val) -> cb.autoUpdatePartitionsInterval(val, TimeUnit.MILLISECONDS));
			map.from(this::getResetIncludeHead).whenTrue().to((b) -> consumerBuilder.startMessageIdInclusive());
			map.from(this::getPoolMessages).to(consumerBuilder::poolMessages);
			map.from(this::getStartPaused).to(consumerBuilder::startPaused);
			mapAcknowledgementProperties(this.getAck(), map, consumerBuilder);
			mapChunkingProperties(this.getChunk(), map, consumerBuilder);
			mapSubscriptionProperties(this.getSubscription(), map, consumerBuilder);
		};
	}

	/**
	 * Maps from a dead letter policy config props to a 'DeadLetterPolicy' expected by
	 * Pulsar.
	 * @param deadLetterPolicyConfig the config props defining the DLP to construct
	 * @return the Pulsar expected dead letter policy
	 */
	private DeadLetterPolicy toPulsarDeadLetterPolicy(DeadLetterPolicyConfig deadLetterPolicyConfig) {
		var map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		var dlpBuilder = DeadLetterPolicy.builder();
		var dlpConfigProps = this.getDeadLetterPolicy();
		map.from(dlpConfigProps::getMaxRedeliverCount).to(dlpBuilder::maxRedeliverCount);
		map.from(dlpConfigProps::getRetryLetterTopic).to(dlpBuilder::retryLetterTopic);
		map.from(dlpConfigProps::getDeadLetterTopic).to(dlpBuilder::deadLetterTopic);
		map.from(dlpConfigProps::getInitialSubscriptionName).to(dlpBuilder::initialSubscriptionName);
		return dlpBuilder.build();
	}

	private void mapAcknowledgementProperties(Acknowledgement ack, PropertyMapper map,
			ConsumerBuilder<?> consumerBuilder) {
		map.from(ack::getBatchIndexEnabled).to(consumerBuilder::enableBatchIndexAcknowledgment);
		map.from(ack::getGroupTime)
			.as(Duration::toMillis)
			.to(consumerBuilder, (cb, val) -> cb.acknowledgmentGroupTime(val, TimeUnit.MILLISECONDS));
		map.from(ack::getReceiptEnabled).to(consumerBuilder::isAckReceiptEnabled);
		map.from(ack::getRedeliveryDelay)
			.as(Duration::toMillis)
			.to(consumerBuilder, (cb, val) -> cb.negativeAckRedeliveryDelay(val, TimeUnit.MILLISECONDS));
		map.from(ack::getTimeout)
			.as(Duration::toMillis)
			.to(consumerBuilder, (cb, val) -> cb.ackTimeout(val, TimeUnit.MILLISECONDS));
		map.from(ack::getTimeoutTickDuration)
			.as(Duration::toMillis)
			.to(consumerBuilder, (cb, val) -> cb.ackTimeoutTickTime(val, TimeUnit.MILLISECONDS));
	}

	private void mapChunkingProperties(Chunking chunk, PropertyMapper map, ConsumerBuilder<?> consumerBuilder) {
		map.from(chunk::getAutoAckOldestOnQueueFull).to(consumerBuilder::autoAckOldestChunkedMessageOnQueueFull);
		map.from(chunk::getExpireTimeIncomplete)
			.as(Duration::toMillis)
			.to(consumerBuilder, (cb, val) -> cb.expireTimeOfIncompleteChunkedMessage(val, TimeUnit.MILLISECONDS));
		map.from(chunk::getMaxPendingMessages).to(consumerBuilder::maxPendingChunkedMessage);
	}

	private void mapSubscriptionProperties(Subscription subscription, PropertyMapper map,
			ConsumerBuilder<?> consumerBuilder) {
		map.from(subscription::getInitialPosition).to(consumerBuilder::subscriptionInitialPosition);
		map.from(subscription::getMode).to(consumerBuilder::subscriptionMode);
		map.from(subscription::getName).to(consumerBuilder::subscriptionName);
		map.from(subscription::getProperties).to(consumerBuilder::subscriptionProperties);
		map.from(subscription::getTopicsMode).to(consumerBuilder::subscriptionTopicsMode);
		map.from(subscription::getReplicateState).to(consumerBuilder::replicateSubscriptionState);
		map.from(subscription::getType).to(consumerBuilder::subscriptionType);
	}

	public static class Acknowledgement {

		/**
		 * Whether the batching index acknowledgment is enabled.
		 */
		private Boolean batchIndexEnabled = false;

		/**
		 * Time to group acknowledgements before sending them to the broker.
		 */
		private Duration groupTime = Duration.ofMillis(100);

		/**
		 * Whether an acknowledgement receipt is enabled.
		 */
		private Boolean receiptEnabled = false;

		/**
		 * Delay before re-delivering messages that have failed to be processed.
		 */
		private Duration redeliveryDelay = Duration.ofMinutes(1);

		/**
		 * Timeout for unacked messages to be redelivered.
		 */
		private Duration timeout = Duration.ZERO;

		/**
		 * Precision for the ack timeout messages tracker.
		 */
		private Duration timeoutTickDuration = Duration.ofSeconds(1);

		public Boolean getBatchIndexEnabled() {
			return this.batchIndexEnabled;
		}

		public void setBatchIndexEnabled(Boolean batchIndexEnabled) {
			this.batchIndexEnabled = batchIndexEnabled;
		}

		public Duration getGroupTime() {
			return this.groupTime;
		}

		public void setGroupTime(Duration groupTime) {
			this.groupTime = groupTime;
		}

		public Boolean getReceiptEnabled() {
			return this.receiptEnabled;
		}

		public void setReceiptEnabled(Boolean receiptEnabled) {
			this.receiptEnabled = receiptEnabled;
		}

		public Duration getRedeliveryDelay() {
			return this.redeliveryDelay;
		}

		public void setRedeliveryDelay(Duration redeliveryDelay) {
			this.redeliveryDelay = redeliveryDelay;
		}

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		public Duration getTimeoutTickDuration() {
			return this.timeoutTickDuration;
		}

		public void setTimeoutTickDuration(Duration timeoutTickDuration) {
			this.timeoutTickDuration = timeoutTickDuration;
		}

	}

	public static class Chunking {

		/**
		 * Whether to automatically drop outstanding uncompleted chunked messages once the
		 * consumer queue reaches the threshold set by the 'maxPendingMessages' property.
		 */
		private Boolean autoAckOldestOnQueueFull = true;

		/**
		 * The maximum time period for a consumer to receive all chunks of a message - if
		 * this threshold is exceeded the consumer will expire the incomplete chunks.
		 */
		private Duration expireTimeIncomplete = Duration.ofMinutes(1);

		/**
		 * Maximum number of chunked messages to be kept in memory.
		 */
		private Integer maxPendingMessages = 10;

		public Boolean getAutoAckOldestOnQueueFull() {
			return this.autoAckOldestOnQueueFull;
		}

		public void setAutoAckOldestOnQueueFull(Boolean autoAckOldestOnQueueFull) {
			this.autoAckOldestOnQueueFull = autoAckOldestOnQueueFull;
		}

		public Duration getExpireTimeIncomplete() {
			return this.expireTimeIncomplete;
		}

		public void setExpireTimeIncomplete(Duration expireTimeIncomplete) {
			this.expireTimeIncomplete = expireTimeIncomplete;
		}

		public Integer getMaxPendingMessages() {
			return this.maxPendingMessages;
		}

		public void setMaxPendingMessages(Integer maxPendingMessages) {
			this.maxPendingMessages = maxPendingMessages;
		}

	}

	public static class Subscription {

		/**
		 * Position where to initialize a newly created subscription.
		 */
		private SubscriptionInitialPosition initialPosition = SubscriptionInitialPosition.Latest;

		/**
		 * Subscription mode to be used when subscribing to the topic.
		 */
		private SubscriptionMode mode = SubscriptionMode.Durable;

		/**
		 * Subscription name for the consumer.
		 */
		private String name;

		/**
		 * Map of properties to add to the subscription.
		 */
		private Map<String, String> properties = new HashMap<>();

		/**
		 * Determines which type of topics (persistent, non-persistent, or all) the
		 * consumer should be subscribed to when using pattern subscriptions.
		 */
		private RegexSubscriptionMode topicsMode = RegexSubscriptionMode.PersistentOnly;

		/**
		 * Whether to replicate subscription state.
		 */
		private Boolean replicateState = false;

		/**
		 * Subscription type to be used when subscribing to a topic.
		 */
		private SubscriptionType type = SubscriptionType.Exclusive;

		public SubscriptionInitialPosition getInitialPosition() {
			return this.initialPosition;
		}

		public void setInitialPosition(SubscriptionInitialPosition initialPosition) {
			this.initialPosition = initialPosition;
		}

		public SubscriptionMode getMode() {
			return this.mode;
		}

		public void setMode(SubscriptionMode mode) {
			this.mode = mode;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}

		public RegexSubscriptionMode getTopicsMode() {
			return this.topicsMode;
		}

		public void setTopicsMode(RegexSubscriptionMode topicsMode) {
			this.topicsMode = topicsMode;
		}

		public Boolean getReplicateState() {
			return this.replicateState;
		}

		public void setReplicateState(Boolean replicateState) {
			this.replicateState = replicateState;
		}

		public SubscriptionType getType() {
			return this.type;
		}

		public void setType(SubscriptionType type) {
			this.type = type;
		}

	}

}
