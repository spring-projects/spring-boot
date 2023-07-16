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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.ProducerCryptoFailureAction;
import org.apache.pulsar.client.api.Range;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.pulsar.reactive.client.api.ImmutableReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.ImmutableReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.ImmutableReactiveMessageSenderSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageSenderSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderSpec;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Spring for the Apache Pulsar reactive client.
 * <p>
 * Users should refer to Pulsar reactive client documentation for complete descriptions of
 * these properties.
 *
 * @author Christophe Bornet
 * @since 3.2.0
 */
@ConfigurationProperties(prefix = "spring.pulsar.reactive")
public class PulsarReactiveProperties {

	private final Sender sender = new Sender();

	private final Consumer consumer = new Consumer();

	private final Reader reader = new Reader();

	private final Listener listener = new Listener();

	public Sender getSender() {
		return this.sender;
	}

	public Consumer getConsumer() {
		return this.consumer;
	}

	public Reader getReader() {
		return this.reader;
	}

	public Listener getListener() {
		return this.listener;
	}

	public ReactiveMessageSenderSpec buildReactiveMessageSenderSpec() {
		return this.sender.buildReactiveMessageSenderSpec();
	}

	public ReactiveMessageReaderSpec buildReactiveMessageReaderSpec() {
		return this.reader.buildReactiveMessageReaderSpec();
	}

	public ReactiveMessageConsumerSpec buildReactiveMessageConsumerSpec() {
		return this.consumer.buildReactiveMessageConsumerSpec();
	}

	public static class Sender {

		/**
		 * Topic the producer will publish to.
		 */
		private String topicName;

		/**
		 * Name for the producer. If not assigned, a unique name is generated.
		 */
		private String producerName;

		/**
		 * Time before a message has to be acknowledged by the broker.
		 */
		private Duration sendTimeout = Duration.ofSeconds(30);

		/**
		 * Maximum number of pending messages for the producer.
		 */
		private Integer maxPendingMessages = 1000;

		/**
		 * Maximum number of pending messages across all the partitions.
		 */
		private Integer maxPendingMessagesAcrossPartitions = 50000;

		/**
		 * Message routing mode for a partitioned producer.
		 */
		private MessageRoutingMode messageRoutingMode = MessageRoutingMode.RoundRobinPartition;

		/**
		 * Message hashing scheme to choose the partition to which the message is
		 * published.
		 */
		private HashingScheme hashingScheme = HashingScheme.JavaStringHash;

		/**
		 * Action the producer will take in case of encryption failure.
		 */
		private ProducerCryptoFailureAction cryptoFailureAction = ProducerCryptoFailureAction.FAIL;

		/**
		 * Time period within which the messages sent will be batched.
		 */
		private Duration batchingMaxPublishDelay = Duration.ofMillis(1);

		/**
		 * Partition switch frequency while batching of messages is enabled and using
		 * round-robin routing mode for non-keyed message.
		 */
		private Integer roundRobinRouterBatchingPartitionSwitchFrequency;

		/**
		 * Maximum number of messages to be batched.
		 */
		private Integer batchingMaxMessages = 1000;

		/**
		 * Maximum number of bytes permitted in a batch.
		 */
		private DataSize batchingMaxBytes = DataSize.ofKilobytes(128);

		/**
		 * Whether to automatically batch messages.
		 */
		private Boolean batchingEnabled = true;

		/**
		 * Whether to split large-size messages into multiple chunks.
		 */
		private Boolean chunkingEnabled = false;

		/**
		 * Names of the public encryption keys to use when encrypting data.
		 */
		private Set<String> encryptionKeys = new HashSet<>();

		/**
		 * Message compression type.
		 */
		private CompressionType compressionType;

		/**
		 * Baseline for the sequence ids for messages published by the producer.
		 */
		private Long initialSequenceId;

		/**
		 * Whether partitioned producer automatically discover new partitions at runtime.
		 */
		private Boolean autoUpdatePartitions = true;

		/**
		 * Interval of partitions discovery updates.
		 */
		private Duration autoUpdatePartitionsInterval = Duration.ofMinutes(1);

		/**
		 * Whether the multiple schema mode is enabled.
		 */
		private Boolean multiSchema = true;

		/**
		 * Type of access to the topic the producer requires.
		 */
		private ProducerAccessMode producerAccessMode = ProducerAccessMode.Shared;

		/**
		 * Whether producers in Shared mode register and connect immediately to the owner
		 * broker of each partition or start lazily on demand.
		 */
		private Boolean lazyStartPartitionedProducers = false;

		/**
		 * Map of properties to add to the producer.
		 */
		private Map<String, String> properties = new HashMap<>();

		private final Cache cache = new Cache();

		public String getTopicName() {
			return this.topicName;
		}

		public void setTopicName(String topicName) {
			this.topicName = topicName;
		}

		public String getProducerName() {
			return this.producerName;
		}

		public void setProducerName(String producerName) {
			this.producerName = producerName;
		}

		public Duration getSendTimeout() {
			return this.sendTimeout;
		}

		public void setSendTimeout(Duration sendTimeout) {
			this.sendTimeout = sendTimeout;
		}

		public Integer getMaxPendingMessages() {
			return this.maxPendingMessages;
		}

		public void setMaxPendingMessages(Integer maxPendingMessages) {
			this.maxPendingMessages = maxPendingMessages;
		}

		public Integer getMaxPendingMessagesAcrossPartitions() {
			return this.maxPendingMessagesAcrossPartitions;
		}

		public void setMaxPendingMessagesAcrossPartitions(Integer maxPendingMessagesAcrossPartitions) {
			this.maxPendingMessagesAcrossPartitions = maxPendingMessagesAcrossPartitions;
		}

		public MessageRoutingMode getMessageRoutingMode() {
			return this.messageRoutingMode;
		}

		public void setMessageRoutingMode(MessageRoutingMode messageRoutingMode) {
			this.messageRoutingMode = messageRoutingMode;
		}

		public HashingScheme getHashingScheme() {
			return this.hashingScheme;
		}

		public void setHashingScheme(HashingScheme hashingScheme) {
			this.hashingScheme = hashingScheme;
		}

		public ProducerCryptoFailureAction getCryptoFailureAction() {
			return this.cryptoFailureAction;
		}

		public void setCryptoFailureAction(ProducerCryptoFailureAction cryptoFailureAction) {
			this.cryptoFailureAction = cryptoFailureAction;
		}

		public Duration getBatchingMaxPublishDelay() {
			return this.batchingMaxPublishDelay;
		}

		public void setBatchingMaxPublishDelay(Duration batchingMaxPublishDelay) {
			this.batchingMaxPublishDelay = batchingMaxPublishDelay;
		}

		public Integer getRoundRobinRouterBatchingPartitionSwitchFrequency() {
			return this.roundRobinRouterBatchingPartitionSwitchFrequency;
		}

		public void setRoundRobinRouterBatchingPartitionSwitchFrequency(
				Integer roundRobinRouterBatchingPartitionSwitchFrequency) {
			this.roundRobinRouterBatchingPartitionSwitchFrequency = roundRobinRouterBatchingPartitionSwitchFrequency;
		}

		public Integer getBatchingMaxMessages() {
			return this.batchingMaxMessages;
		}

		public void setBatchingMaxMessages(Integer batchingMaxMessages) {
			this.batchingMaxMessages = batchingMaxMessages;
		}

		public DataSize getBatchingMaxBytes() {
			return this.batchingMaxBytes;
		}

		public void setBatchingMaxBytes(DataSize batchingMaxBytes) {
			this.batchingMaxBytes = batchingMaxBytes;
		}

		public Boolean getBatchingEnabled() {
			return this.batchingEnabled;
		}

		public void setBatchingEnabled(Boolean batchingEnabled) {
			this.batchingEnabled = batchingEnabled;
		}

		public Boolean getChunkingEnabled() {
			return this.chunkingEnabled;
		}

		public void setChunkingEnabled(Boolean chunkingEnabled) {
			this.chunkingEnabled = chunkingEnabled;
		}

		public Set<String> getEncryptionKeys() {
			return this.encryptionKeys;
		}

		public void setEncryptionKeys(Set<String> encryptionKeys) {
			this.encryptionKeys = encryptionKeys;
		}

		public CompressionType getCompressionType() {
			return this.compressionType;
		}

		public void setCompressionType(CompressionType compressionType) {
			this.compressionType = compressionType;
		}

		public Long getInitialSequenceId() {
			return this.initialSequenceId;
		}

		public void setInitialSequenceId(Long initialSequenceId) {
			this.initialSequenceId = initialSequenceId;
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

		public Boolean getMultiSchema() {
			return this.multiSchema;
		}

		public void setMultiSchema(Boolean multiSchema) {
			this.multiSchema = multiSchema;
		}

		public ProducerAccessMode getProducerAccessMode() {
			return this.producerAccessMode;
		}

		public void setProducerAccessMode(ProducerAccessMode producerAccessMode) {
			this.producerAccessMode = producerAccessMode;
		}

		public Boolean getLazyStartPartitionedProducers() {
			return this.lazyStartPartitionedProducers;
		}

		public void setLazyStartPartitionedProducers(Boolean lazyStartPartitionedProducers) {
			this.lazyStartPartitionedProducers = lazyStartPartitionedProducers;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}

		public Cache getCache() {
			return this.cache;
		}

		public ReactiveMessageSenderSpec buildReactiveMessageSenderSpec() {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

			MutableReactiveMessageSenderSpec spec = new MutableReactiveMessageSenderSpec();

			map.from(this::getTopicName).to(spec::setTopicName);
			map.from(this::getProducerName).to(spec::setProducerName);
			map.from(this::getSendTimeout).to(spec::setSendTimeout);
			map.from(this::getMaxPendingMessages).to(spec::setMaxPendingMessages);
			map.from(this::getMaxPendingMessagesAcrossPartitions).to(spec::setMaxPendingMessagesAcrossPartitions);
			map.from(this::getMessageRoutingMode).to(spec::setMessageRoutingMode);
			map.from(this::getHashingScheme).to(spec::setHashingScheme);
			map.from(this::getCryptoFailureAction).to(spec::setCryptoFailureAction);
			map.from(this::getBatchingMaxPublishDelay).to(spec::setBatchingMaxPublishDelay);
			map.from(this::getRoundRobinRouterBatchingPartitionSwitchFrequency)
				.to(spec::setRoundRobinRouterBatchingPartitionSwitchFrequency);
			map.from(this::getBatchingMaxMessages).to(spec::setBatchingMaxMessages);
			map.from(this::getBatchingMaxBytes).asInt(DataSize::toBytes).to(spec::setBatchingMaxBytes);
			map.from(this::getBatchingEnabled).to(spec::setBatchingEnabled);
			map.from(this::getChunkingEnabled).to(spec::setChunkingEnabled);
			map.from(this::getEncryptionKeys).to(spec::setEncryptionKeys);
			map.from(this::getCompressionType).to(spec::setCompressionType);
			map.from(this::getInitialSequenceId).to(spec::setInitialSequenceId);
			map.from(this::getAutoUpdatePartitions).to(spec::setAutoUpdatePartitions);
			map.from(this::getAutoUpdatePartitionsInterval).to(spec::setAutoUpdatePartitionsInterval);
			map.from(this::getMultiSchema).to(spec::setMultiSchema);
			map.from(this::getProducerAccessMode).to(spec::setAccessMode);
			map.from(this::getLazyStartPartitionedProducers).to(spec::setLazyStartPartitionedProducers);
			map.from(this::getProperties).to(spec::setProperties);

			return new ImmutableReactiveMessageSenderSpec(spec);
		}

	}

	public static class Reader {

		/**
		 * Topic names.
		 */
		private String[] topicNames;

		/**
		 * Reader name.
		 */
		private String readerName;

		/**
		 * Subscription name.
		 */
		private String subscriptionName;

		/**
		 * Prefix to use when auto-generating a subscription name.
		 */
		private String generatedSubscriptionNamePrefix;

		/**
		 * Size of a consumer's receiver queue.
		 */
		private Integer receiverQueueSize;

		/**
		 * Whether to read messages from a compacted topic rather than a full message
		 * backlog of a topic.
		 */
		private Boolean readCompacted;

		/**
		 * Key hash ranges of the reader.
		 */
		private Range[] keyHashRanges;

		/**
		 * Action the reader will take in case of decryption failures.
		 */
		private ConsumerCryptoFailureAction cryptoFailureAction;

		public String[] getTopicNames() {
			return this.topicNames;
		}

		public void setTopicNames(String[] topicNames) {
			this.topicNames = topicNames;
		}

		public String getReaderName() {
			return this.readerName;
		}

		public void setReaderName(String readerName) {
			this.readerName = readerName;
		}

		public String getSubscriptionName() {
			return this.subscriptionName;
		}

		public void setSubscriptionName(String subscriptionName) {
			this.subscriptionName = subscriptionName;
		}

		public String getGeneratedSubscriptionNamePrefix() {
			return this.generatedSubscriptionNamePrefix;
		}

		public void setGeneratedSubscriptionNamePrefix(String generatedSubscriptionNamePrefix) {
			this.generatedSubscriptionNamePrefix = generatedSubscriptionNamePrefix;
		}

		public Integer getReceiverQueueSize() {
			return this.receiverQueueSize;
		}

		public void setReceiverQueueSize(Integer receiverQueueSize) {
			this.receiverQueueSize = receiverQueueSize;
		}

		public Boolean getReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(Boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public Range[] getKeyHashRanges() {
			return this.keyHashRanges;
		}

		public void setKeyHashRanges(Range[] keyHashRanges) {
			this.keyHashRanges = keyHashRanges;
		}

		public ConsumerCryptoFailureAction getCryptoFailureAction() {
			return this.cryptoFailureAction;
		}

		public void setCryptoFailureAction(ConsumerCryptoFailureAction cryptoFailureAction) {
			this.cryptoFailureAction = cryptoFailureAction;
		}

		public ReactiveMessageReaderSpec buildReactiveMessageReaderSpec() {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

			MutableReactiveMessageReaderSpec spec = new MutableReactiveMessageReaderSpec();

			map.from(this::getTopicNames).as(List::of).to(spec::setTopicNames);
			map.from(this::getReaderName).to(spec::setReaderName);
			map.from(this::getSubscriptionName).to(spec::setSubscriptionName);
			map.from(this::getGeneratedSubscriptionNamePrefix).to(spec::setGeneratedSubscriptionNamePrefix);
			map.from(this::getReceiverQueueSize).to(spec::setReceiverQueueSize);
			map.from(this::getReadCompacted).to(spec::setReadCompacted);
			map.from(this::getKeyHashRanges).as(List::of).to(spec::setKeyHashRanges);
			map.from(this::getCryptoFailureAction).to(spec::setCryptoFailureAction);

			return new ImmutableReactiveMessageReaderSpec(spec);
		}

	}

	public static class Consumer {

		/**
		 * Comma-separated list of topics the consumer subscribes to.
		 */
		private String[] topics;

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
		private SortedMap<String, String> subscriptionProperties = new TreeMap<>();

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
		 * When set to true, ignores the acknowledge operation completion and makes it
		 * asynchronous from the message consuming processing to improve performance by
		 * allowing the acknowledges and message processing to interleave. Defaults to
		 * true.
		 */
		private Boolean acknowledgeAsynchronously = true;

		/**
		 * Type of acknowledge scheduler.
		 */
		private SchedulerType acknowledgeSchedulerType;

		/**
		 * Delay before re-delivering messages that have failed to be processed.
		 */
		private Duration negativeAckRedeliveryDelay = Duration.ofMinutes(1);

		/**
		 * Configuration for the dead letter queue.
		 */
		@NestedConfigurationProperty
		private DeadLetterPolicy deadLetterPolicy;

		/**
		 * Whether the retry letter topic is enabled.
		 */
		private Boolean retryLetterTopicEnable = false;

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
		private Duration ackTimeoutTickTime = Duration.ofSeconds(1);

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
		 * Whether batch index acknowledgement is enabled.
		 */
		private Boolean batchIndexAckEnabled = false;

		/**
		 * Position where to initialize a newly created subscription.
		 */
		private SubscriptionInitialPosition subscriptionInitialPosition = SubscriptionInitialPosition.Latest;

		/**
		 * Auto-discovery period for topics when topic pattern is used.
		 */
		private Duration topicsPatternAutoDiscoveryPeriod = Duration.ofMinutes(1);

		/**
		 * Determines which topics the consumer should be subscribed to when using pattern
		 * subscriptions.
		 */
		private RegexSubscriptionMode topicsPatternSubscriptionMode = RegexSubscriptionMode.PersistentOnly;

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
		 * Whether to automatically drop outstanding un-acked messages if the queue is
		 * full.
		 */
		private Boolean autoAckOldestChunkedMessageOnQueueFull = true;

		/**
		 * Maximum number of chunked messages to be kept in memory.
		 */
		private Integer maxPendingChunkedMessage = 10;

		/**
		 * Time to expire incomplete chunks if the consumer won't be able to receive all
		 * chunks before in milliseconds.
		 */
		private Duration expireTimeOfIncompleteChunkedMessage = Duration.ofMinutes(1);

		public String[] getTopics() {
			return this.topics;
		}

		public void setTopics(String[] topics) {
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

		public SubscriptionType getSubscriptionType() {
			return this.subscriptionType;
		}

		public void setSubscriptionType(SubscriptionType subscriptionType) {
			this.subscriptionType = subscriptionType;
		}

		public SortedMap<String, String> getSubscriptionProperties() {
			return this.subscriptionProperties;
		}

		public void setSubscriptionProperties(SortedMap<String, String> subscriptionProperties) {
			this.subscriptionProperties = subscriptionProperties;
		}

		public SubscriptionMode getSubscriptionMode() {
			return this.subscriptionMode;
		}

		public void setSubscriptionMode(SubscriptionMode subscriptionMode) {
			this.subscriptionMode = subscriptionMode;
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

		public Boolean getAcknowledgeAsynchronously() {
			return this.acknowledgeAsynchronously;
		}

		public void setAcknowledgeAsynchronously(Boolean acknowledgeAsynchronously) {
			this.acknowledgeAsynchronously = acknowledgeAsynchronously;
		}

		public SchedulerType getAcknowledgeSchedulerType() {
			return this.acknowledgeSchedulerType;
		}

		public void setAcknowledgeSchedulerType(SchedulerType acknowledgeSchedulerType) {
			this.acknowledgeSchedulerType = acknowledgeSchedulerType;
		}

		public Duration getNegativeAckRedeliveryDelay() {
			return this.negativeAckRedeliveryDelay;
		}

		public void setNegativeAckRedeliveryDelay(Duration negativeAckRedeliveryDelay) {
			this.negativeAckRedeliveryDelay = negativeAckRedeliveryDelay;
		}

		public DeadLetterPolicy getDeadLetterPolicy() {
			return this.deadLetterPolicy;
		}

		public void setDeadLetterPolicy(DeadLetterPolicy deadLetterPolicy) {
			this.deadLetterPolicy = deadLetterPolicy;
		}

		public Boolean getRetryLetterTopicEnable() {
			return this.retryLetterTopicEnable;
		}

		public void setRetryLetterTopicEnable(Boolean retryLetterTopicEnable) {
			this.retryLetterTopicEnable = retryLetterTopicEnable;
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

		public Duration getAckTimeoutTickTime() {
			return this.ackTimeoutTickTime;
		}

		public void setAckTimeoutTickTime(Duration ackTimeoutTickTime) {
			this.ackTimeoutTickTime = ackTimeoutTickTime;
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

		public Boolean getBatchIndexAckEnabled() {
			return this.batchIndexAckEnabled;
		}

		public void setBatchIndexAckEnabled(Boolean batchIndexAckEnabled) {
			this.batchIndexAckEnabled = batchIndexAckEnabled;
		}

		public SubscriptionInitialPosition getSubscriptionInitialPosition() {
			return this.subscriptionInitialPosition;
		}

		public void setSubscriptionInitialPosition(SubscriptionInitialPosition subscriptionInitialPosition) {
			this.subscriptionInitialPosition = subscriptionInitialPosition;
		}

		public Duration getTopicsPatternAutoDiscoveryPeriod() {
			return this.topicsPatternAutoDiscoveryPeriod;
		}

		public void setTopicsPatternAutoDiscoveryPeriod(Duration topicsPatternAutoDiscoveryPeriod) {
			this.topicsPatternAutoDiscoveryPeriod = topicsPatternAutoDiscoveryPeriod;
		}

		public RegexSubscriptionMode getTopicsPatternSubscriptionMode() {
			return this.topicsPatternSubscriptionMode;
		}

		public void setTopicsPatternSubscriptionMode(RegexSubscriptionMode topicsPatternSubscriptionMode) {
			this.topicsPatternSubscriptionMode = topicsPatternSubscriptionMode;
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

		public ReactiveMessageConsumerSpec buildReactiveMessageConsumerSpec() {

			MutableReactiveMessageConsumerSpec spec = new MutableReactiveMessageConsumerSpec();

			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

			map.from(this::getTopics).as(List::of).to(spec::setTopicNames);
			map.from(this::getTopicsPattern).to(spec::setTopicsPattern);
			map.from(this::getSubscriptionName).to(spec::setSubscriptionName);
			map.from(this::getSubscriptionType).to(spec::setSubscriptionType);
			map.from(this::getSubscriptionProperties).to(spec::setSubscriptionProperties);
			map.from(this::getSubscriptionMode).to(spec::setSubscriptionMode);
			map.from(this::getReceiverQueueSize).to(spec::setReceiverQueueSize);
			map.from(this::getAcknowledgementsGroupTime).to(spec::setAcknowledgementsGroupTime);
			map.from(this::getAcknowledgeAsynchronously).to(spec::setAcknowledgeAsynchronously);
			map.from(this::getAcknowledgeSchedulerType).as((scheduler) -> switch (scheduler) {
				case boundedElastic -> Schedulers.boundedElastic();
				case parallel -> Schedulers.parallel();
				case single -> Schedulers.single();
				case immediate -> Schedulers.immediate();
			}).to(spec::setAcknowledgeScheduler);
			map.from(this::getNegativeAckRedeliveryDelay).to(spec::setNegativeAckRedeliveryDelay);
			map.from(this::getDeadLetterPolicy).to(spec::setDeadLetterPolicy);
			map.from(this::getRetryLetterTopicEnable).to(spec::setRetryLetterTopicEnable);
			map.from(this::getMaxTotalReceiverQueueSizeAcrossPartitions)
				.to(spec::setMaxTotalReceiverQueueSizeAcrossPartitions);
			map.from(this::getConsumerName).to(spec::setConsumerName);
			map.from(this::getAckTimeout).to(spec::setAckTimeout);
			map.from(this::getAckTimeoutTickTime).to(spec::setAckTimeoutTickTime);
			map.from(this::getPriorityLevel).to(spec::setPriorityLevel);
			map.from(this::getCryptoFailureAction).to(spec::setCryptoFailureAction);
			map.from(this::getProperties).to(spec::setProperties);
			map.from(this::getReadCompacted).to(spec::setReadCompacted);
			map.from(this::getBatchIndexAckEnabled).to(spec::setBatchIndexAckEnabled);
			map.from(this::getSubscriptionInitialPosition).to(spec::setSubscriptionInitialPosition);
			map.from(this::getTopicsPatternAutoDiscoveryPeriod).to(spec::setTopicsPatternAutoDiscoveryPeriod);
			map.from(this::getTopicsPatternSubscriptionMode).to(spec::setTopicsPatternSubscriptionMode);
			map.from(this::getAutoUpdatePartitions).to(spec::setAutoUpdatePartitions);
			map.from(this::getAutoUpdatePartitionsInterval).to(spec::setAutoUpdatePartitionsInterval);
			map.from(this::getReplicateSubscriptionState).to(spec::setReplicateSubscriptionState);
			map.from(this::getAutoAckOldestChunkedMessageOnQueueFull)
				.to(spec::setAutoAckOldestChunkedMessageOnQueueFull);
			map.from(this::getMaxPendingChunkedMessage).to(spec::setMaxPendingChunkedMessage);
			map.from(this::getExpireTimeOfIncompleteChunkedMessage).to(spec::setExpireTimeOfIncompleteChunkedMessage);
			return new ImmutableReactiveMessageConsumerSpec(spec);
		}

	}

	public enum SchedulerType {

		/**
		 * The reactor.core.scheduler.BoundedElasticScheduler.
		 */
		boundedElastic,

		/**
		 * The reactor.core.scheduler.ParallelScheduler.
		 */
		parallel,

		/**
		 * The reactor.core.scheduler.SingleScheduler.
		 */
		single,

		/**
		 * The reactor.core.scheduler.ImmediateScheduler.
		 */
		immediate

	}

	public static class Cache {

		/** Time period after last access to expire unused entries in the cache. */
		private Duration expireAfterAccess = Duration.ofMinutes(1);

		/** Time period after last write to expire unused entries in the cache. */
		private Duration expireAfterWrite = Duration.ofMinutes(10);

		/** Maximum size of cache (entries). */
		private Long maximumSize = 1000L;

		/** Initial size of cache. */
		private Integer initialCapacity = 50;

		public Duration getExpireAfterAccess() {
			return this.expireAfterAccess;
		}

		public void setExpireAfterAccess(Duration expireAfterAccess) {
			this.expireAfterAccess = expireAfterAccess;
		}

		public Duration getExpireAfterWrite() {
			return this.expireAfterWrite;
		}

		public void setExpireAfterWrite(Duration expireAfterWrite) {
			this.expireAfterWrite = expireAfterWrite;
		}

		public Long getMaximumSize() {
			return this.maximumSize;
		}

		public void setMaximumSize(Long maximumSize) {
			this.maximumSize = maximumSize;
		}

		public Integer getInitialCapacity() {
			return this.initialCapacity;
		}

		public void setInitialCapacity(Integer initialCapacity) {
			this.initialCapacity = initialCapacity;
		}

	}

	public static class Listener {

		/**
		 * SchemaType of the consumed messages.
		 */
		private SchemaType schemaType;

		/**
		 * Duration to wait before the message handling times out.
		 */
		private Duration handlingTimeout = Duration.ofMinutes(2);

		/**
		 * Whether per-key message ordering should be maintained when concurrent
		 * processing is used.
		 */
		private Boolean useKeyOrderedProcessing = false;

		public SchemaType getSchemaType() {
			return this.schemaType;
		}

		public void setSchemaType(SchemaType schemaType) {
			this.schemaType = schemaType;
		}

		public Duration getHandlingTimeout() {
			return this.handlingTimeout;
		}

		public void setHandlingTimeout(Duration handlingTimeout) {
			this.handlingTimeout = handlingTimeout;
		}

		public Boolean getUseKeyOrderedProcessing() {
			return this.useKeyOrderedProcessing;
		}

		public void setUseKeyOrderedProcessing(Boolean useKeyOrderedProcessing) {
			this.useKeyOrderedProcessing = useKeyOrderedProcessing;
		}

	}

}
