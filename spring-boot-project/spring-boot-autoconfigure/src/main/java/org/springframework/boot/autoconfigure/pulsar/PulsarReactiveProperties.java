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
 * @author Chris Bono
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

		private final Batching batch = new Batching();

		/**
		 * Topic the producer will publish to.
		 */
		private String topicName;

		/**
		 * Name for the producer. If not assigned, a unique name is generated.
		 */
		private String name;

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
		private ProducerAccessMode accessMode = ProducerAccessMode.Shared;

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

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
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

		public Batching getBatch() {
			return this.batch;
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

		public ProducerAccessMode getAccessMode() {
			return this.accessMode;
		}

		public void setAccessMode(ProducerAccessMode accessMode) {
			this.accessMode = accessMode;
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
			map.from(this::getName).to(spec::setProducerName);
			map.from(this::getSendTimeout).to(spec::setSendTimeout);
			map.from(this::getMaxPendingMessages).to(spec::setMaxPendingMessages);
			map.from(this::getMaxPendingMessagesAcrossPartitions).to(spec::setMaxPendingMessagesAcrossPartitions);
			map.from(this::getMessageRoutingMode).to(spec::setMessageRoutingMode);
			map.from(this::getHashingScheme).to(spec::setHashingScheme);
			map.from(this::getCryptoFailureAction).to(spec::setCryptoFailureAction);
			map.from(this::getBatch).as(Batching::getMaxPublishDelay).to(spec::setBatchingMaxPublishDelay);
			map.from(this::getBatch)
				.as(Batching::getRoundRobinRouterPartitionSwitchFrequency)
				.to(spec::setRoundRobinRouterBatchingPartitionSwitchFrequency);
			map.from(this::getBatch).as(Batching::getMaxMessages).to(spec::setBatchingMaxMessages);
			map.from(this::getBatch).as(Batching::getMaxBytes).asInt(DataSize::toBytes).to(spec::setBatchingMaxBytes);
			map.from(this::getBatch).as(Batching::getEnabled).to(spec::setBatchingEnabled);
			map.from(this::getChunkingEnabled).to(spec::setChunkingEnabled);
			map.from(this::getEncryptionKeys).to(spec::setEncryptionKeys);
			map.from(this::getCompressionType).to(spec::setCompressionType);
			map.from(this::getInitialSequenceId).to(spec::setInitialSequenceId);
			map.from(this::getAutoUpdatePartitions).to(spec::setAutoUpdatePartitions);
			map.from(this::getAutoUpdatePartitionsInterval).to(spec::setAutoUpdatePartitionsInterval);
			map.from(this::getMultiSchema).to(spec::setMultiSchema);
			map.from(this::getAccessMode).to(spec::setAccessMode);
			map.from(this::getLazyStartPartitionedProducers).to(spec::setLazyStartPartitionedProducers);
			map.from(this::getProperties).to(spec::setProperties);
			return new ImmutableReactiveMessageSenderSpec(spec);
		}

	}

	public static class Batching {

		/**
		 * Time period within which the messages sent will be batched.
		 */
		private Duration maxPublishDelay = Duration.ofMillis(1);

		/**
		 * Partition switch frequency while batching of messages is enabled and using
		 * round-robin routing mode for non-keyed message.
		 */
		private Integer roundRobinRouterPartitionSwitchFrequency;

		/**
		 * Maximum number of messages to be batched.
		 */
		private Integer maxMessages = 1000;

		/**
		 * Maximum number of bytes permitted in a batch.
		 */
		private DataSize maxBytes = DataSize.ofKilobytes(128);

		/**
		 * Whether to automatically batch messages.
		 */
		private Boolean enabled = true;

		public Duration getMaxPublishDelay() {
			return this.maxPublishDelay;
		}

		public void setMaxPublishDelay(Duration maxPublishDelay) {
			this.maxPublishDelay = maxPublishDelay;
		}

		public Integer getRoundRobinRouterPartitionSwitchFrequency() {
			return this.roundRobinRouterPartitionSwitchFrequency;
		}

		public void setRoundRobinRouterPartitionSwitchFrequency(Integer roundRobinRouterPartitionSwitchFrequency) {
			this.roundRobinRouterPartitionSwitchFrequency = roundRobinRouterPartitionSwitchFrequency;
		}

		public Integer getMaxMessages() {
			return this.maxMessages;
		}

		public void setMaxMessages(Integer maxMessages) {
			this.maxMessages = maxMessages;
		}

		public DataSize getMaxBytes() {
			return this.maxBytes;
		}

		public void setMaxBytes(DataSize maxBytes) {
			this.maxBytes = maxBytes;
		}

		public Boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
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
		private String name;

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

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
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
			map.from(this::getName).to(spec::setReaderName);
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

		private final Acknowledgement ack = new Acknowledgement();

		private final Chunking chunk = new Chunking();

		private final Subscription subscription = new Subscription();

		/**
		 * Comma-separated list of topics the consumer subscribes to.
		 */
		private String[] topics;

		/**
		 * Pattern for topics the consumer subscribes to.
		 */
		private Pattern topicsPattern;

		/**
		 * Number of messages that can be accumulated before the consumer calls "receive".
		 */
		private Integer receiverQueueSize = 1000;

		/**
		 * Dead letter policy to use.
		 */
		@NestedConfigurationProperty
		private DeadLetterPolicyConfig deadLetterPolicy = new DeadLetterPolicyConfig();

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

		public Integer getReceiverQueueSize() {
			return this.receiverQueueSize;
		}

		public void setReceiverQueueSize(Integer receiverQueueSize) {
			this.receiverQueueSize = receiverQueueSize;
		}

		public DeadLetterPolicyConfig getDeadLetterPolicy() {
			return this.deadLetterPolicy;
		}

		public void setDeadLetterPolicy(DeadLetterPolicyConfig deadLetterPolicy) {
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

		public Acknowledgement getAck() {
			return this.ack;
		}

		public Chunking getChunk() {
			return this.chunk;
		}

		public Subscription getSubscription() {
			return this.subscription;
		}

		public ReactiveMessageConsumerSpec buildReactiveMessageConsumerSpec() {
			MutableReactiveMessageConsumerSpec spec = new MutableReactiveMessageConsumerSpec();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getTopics).as(List::of).to(spec::setTopicNames);
			map.from(this::getTopicsPattern).to(spec::setTopicsPattern);
			map.from(this::getDeadLetterPolicy).as(this::toPulsarDeadLetterPolicy).to(spec::setDeadLetterPolicy);
			map.from(this::getRetryLetterTopicEnable).to(spec::setRetryLetterTopicEnable);
			map.from(this::getMaxTotalReceiverQueueSizeAcrossPartitions)
				.to(spec::setMaxTotalReceiverQueueSizeAcrossPartitions);
			map.from(this::getName).to(spec::setConsumerName);
			map.from(this::getPriorityLevel).to(spec::setPriorityLevel);
			map.from(this::getCryptoFailureAction).to(spec::setCryptoFailureAction);
			map.from(this::getProperties).to(spec::setProperties);
			map.from(this::getReadCompacted).to(spec::setReadCompacted);
			map.from(this::getTopicsPatternAutoDiscoveryPeriod).to(spec::setTopicsPatternAutoDiscoveryPeriod);
			map.from(this::getTopicsPatternSubscriptionMode).to(spec::setTopicsPatternSubscriptionMode);
			map.from(this::getAutoUpdatePartitions).to(spec::setAutoUpdatePartitions);
			map.from(this::getAutoUpdatePartitionsInterval).to(spec::setAutoUpdatePartitionsInterval);
			map.from(this::getReceiverQueueSize).to(spec::setReceiverQueueSize);
			mapAcknowledgementProperties(this.getAck(), map, spec);
			mapChunkingProperties(this.getChunk(), map, spec);
			mapSubscriptionProperties(this.getSubscription(), map, spec);
			return new ImmutableReactiveMessageConsumerSpec(spec);
		}

		/**
		 * Maps from a dead letter policy config props to a 'DeadLetterPolicy' expected by
		 * Pulsar.
		 * @param dlpConfigProps the config props defining the DLP to construct
		 * @return the Pulsar expected dead letter policy
		 */
		private DeadLetterPolicy toPulsarDeadLetterPolicy(DeadLetterPolicyConfig dlpConfigProps) {
			var map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			var dlpBuilder = DeadLetterPolicy.builder();
			map.from(dlpConfigProps::getMaxRedeliverCount).to(dlpBuilder::maxRedeliverCount);
			map.from(dlpConfigProps::getRetryLetterTopic).to(dlpBuilder::retryLetterTopic);
			map.from(dlpConfigProps::getDeadLetterTopic).to(dlpBuilder::deadLetterTopic);
			map.from(dlpConfigProps::getInitialSubscriptionName).to(dlpBuilder::initialSubscriptionName);
			return dlpBuilder.build();
		}

		private void mapAcknowledgementProperties(Acknowledgement ack, PropertyMapper map,
				MutableReactiveMessageConsumerSpec spec) {
			map.from(ack::getGroupTime).to(spec::setAcknowledgementsGroupTime);
			map.from(ack::getAsync).to(spec::setAcknowledgeAsynchronously);
			map.from(ack::getSchedulerType).as((scheduler) -> switch (scheduler) {
				case boundedElastic -> Schedulers.boundedElastic();
				case parallel -> Schedulers.parallel();
				case single -> Schedulers.single();
				case immediate -> Schedulers.immediate();
			}).to(spec::setAcknowledgeScheduler);
			map.from(ack::getRedeliveryDelay).to(spec::setNegativeAckRedeliveryDelay);
			map.from(ack::getTimeout).to(spec::setAckTimeout);
			map.from(ack::getTimeoutTickTime).to(spec::setAckTimeoutTickTime);
			map.from(ack::getBatchIndexEnabled).to(spec::setBatchIndexAckEnabled);
		}

		private void mapChunkingProperties(Chunking chunk, PropertyMapper map,
				MutableReactiveMessageConsumerSpec spec) {
			map.from(chunk::getAutoAckOldestOnQueueFull).to(spec::setAutoAckOldestChunkedMessageOnQueueFull);
			map.from(chunk::getMaxPendingMessages).to(spec::setMaxPendingChunkedMessage);
			map.from(chunk::getExpireTimeIncomplete).to(spec::setExpireTimeOfIncompleteChunkedMessage);
		}

		private void mapSubscriptionProperties(Subscription subscription, PropertyMapper map,
				MutableReactiveMessageConsumerSpec spec) {
			map.from(subscription::getName).to(spec::setSubscriptionName);
			map.from(subscription::getType).to(spec::setSubscriptionType);
			map.from(subscription::getProperties).to(spec::setSubscriptionProperties);
			map.from(subscription::getMode).to(spec::setSubscriptionMode);
			map.from(subscription::getInitialPosition).to(spec::setSubscriptionInitialPosition);
			map.from(subscription::getReplicateState).to(spec::setReplicateSubscriptionState);
		}

	}

	public static class Acknowledgement {

		/**
		 * Time to group acknowledgements before sending them to the broker.
		 */
		private Duration groupTime = Duration.ofMillis(100);

		/**
		 * When set to true, ignores the acknowledge operation completion and makes it
		 * asynchronous from the message consuming processing to improve performance by
		 * allowing the acknowledges and message processing to interleave. Defaults to
		 * true.
		 */
		private Boolean async = true;

		/**
		 * Type of acknowledge scheduler.
		 */
		private SchedulerType schedulerType;

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
		private Duration timeoutTickTime = Duration.ofSeconds(1);

		/**
		 * Whether batch index acknowledgement is enabled.
		 */
		private Boolean batchIndexEnabled = false;

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

		public Boolean getAsync() {
			return this.async;
		}

		public void setAsync(Boolean async) {
			this.async = async;
		}

		public SchedulerType getSchedulerType() {
			return this.schedulerType;
		}

		public void setSchedulerType(SchedulerType schedulerType) {
			this.schedulerType = schedulerType;
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

		public Duration getTimeoutTickTime() {
			return this.timeoutTickTime;
		}

		public void setTimeoutTickTime(Duration timeoutTickTime) {
			this.timeoutTickTime = timeoutTickTime;
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
		 * Whether to replicate subscription state.
		 */
		private Boolean replicateState = false;

		/**
		 * Subscription name for the consumer.
		 */
		private String name;

		/**
		 * Subscription type to be used when subscribing to a topic.
		 */
		private SubscriptionType type = SubscriptionType.Exclusive;

		/**
		 * Map of properties to add to the subscription.
		 */
		private SortedMap<String, String> properties = new TreeMap<>();

		/**
		 * Subscription mode to be used when subscribing to the topic.
		 */
		private SubscriptionMode mode = SubscriptionMode.Durable;

		public SubscriptionInitialPosition getInitialPosition() {
			return this.initialPosition;
		}

		public void setInitialPosition(SubscriptionInitialPosition initialPosition) {
			this.initialPosition = initialPosition;
		}

		public Boolean getReplicateState() {
			return this.replicateState;
		}

		public void setReplicateState(Boolean replicateState) {
			this.replicateState = replicateState;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public SubscriptionType getType() {
			return this.type;
		}

		public void setType(SubscriptionType type) {
			this.type = type;
		}

		public SortedMap<String, String> getProperties() {
			return this.properties;
		}

		public void setProperties(SortedMap<String, String> properties) {
			this.properties = properties;
		}

		public SubscriptionMode getMode() {
			return this.mode;
		}

		public void setMode(SubscriptionMode mode) {
			this.mode = mode;
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
