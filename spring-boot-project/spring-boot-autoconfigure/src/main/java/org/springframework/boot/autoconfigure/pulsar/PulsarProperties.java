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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.ProducerCryptoFailureAction;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.PulsarAdminBuilderCustomizer;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.listener.AckMode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Spring for Apache Pulsar.
 * <p>
 * Users should refer to Pulsar documentation for complete descriptions of these
 * properties.
 *
 * @author Soby Chacko
 * @author Alexander Preuß
 * @author Christophe Bornet
 * @author Chris Bono
 * @author Kevin Lu
 * @since 3.2.0
 */
@ConfigurationProperties(prefix = "spring.pulsar")
public class PulsarProperties {

	private final ConsumerConfigProperties consumer = new ConsumerConfigProperties();

	private final Client client = new Client();

	private final Function function = new Function();

	private final Listener listener = new Listener();

	private final ProducerConfigProperties producer = new ProducerConfigProperties();

	private final Template template = new Template();

	private final Admin admin = new Admin();

	private final Reader reader = new Reader();

	private final Defaults defaults = new Defaults();

	public ConsumerConfigProperties getConsumer() {
		return this.consumer;
	}

	public Client getClient() {
		return this.client;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Function getFunction() {
		return this.function;
	}

	public ProducerConfigProperties getProducer() {
		return this.producer;
	}

	public Template getTemplate() {
		return this.template;
	}

	public Admin getAdministration() {
		return this.admin;
	}

	public Reader getReader() {
		return this.reader;
	}

	public Defaults getDefaults() {
		return this.defaults;
	}

	public static class Template {

		/**
		 * Whether to record observations for send operations when the Observations API is
		 * available.
		 */
		private boolean observationsEnabled = true;

		public Boolean isObservationsEnabled() {
			return this.observationsEnabled;
		}

		public void setObservationsEnabled(boolean observationsEnabled) {
			this.observationsEnabled = observationsEnabled;
		}

	}

	public static class Cache {

		/** Time period to expire unused entries in the cache. */
		private Duration expireAfterAccess = Duration.ofMinutes(1);

		/** Maximum size of cache (entries). */
		private long maximumSize = 1000L;

		/** Initial size of cache. */
		private int initialCapacity = 50;

		public Duration getExpireAfterAccess() {
			return this.expireAfterAccess;
		}

		public void setExpireAfterAccess(Duration expireAfterAccess) {
			this.expireAfterAccess = expireAfterAccess;
		}

		public long getMaximumSize() {
			return this.maximumSize;
		}

		public void setMaximumSize(long maximumSize) {
			this.maximumSize = maximumSize;
		}

		public int getInitialCapacity() {
			return this.initialCapacity;
		}

		public void setInitialCapacity(int initialCapacity) {
			this.initialCapacity = initialCapacity;
		}

	}

	public static class ConsumerConfigProperties {

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
		private int receiverQueueSize = 1000;

		/**
		 * Maximum number of messages that a consumer can be pushed at once from a broker
		 * across all partitions.
		 */
		private int maxTotalReceiverQueueSizeAcrossPartitions = 50000;

		/**
		 * Consumer name to identify a particular consumer from the topic stats.
		 */
		private String name;

		/**
		 * Priority level for shared subscription consumers.
		 */
		private int priorityLevel = 0;

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
		private boolean readCompacted = false;

		/**
		 * Auto-discovery period for topics when topic pattern is used in minutes.
		 */
		private int patternAutoDiscoveryPeriod = 1;

		/**
		 * Dead letter policy to use.
		 */
		@NestedConfigurationProperty
		private DeadLetterPolicyConfig deadLetterPolicy = new DeadLetterPolicyConfig();

		/**
		 * Whether to auto retry messages.
		 */
		private boolean retryEnable = false;

		/**
		 * Whether the consumer auto-subscribes for partition increase. This is only for
		 * partitioned consumers.
		 */
		private boolean autoUpdatePartitions = true;

		/**
		 * Interval of partitions discovery updates.
		 */
		private Duration autoUpdatePartitionsInterval = Duration.ofMinutes(1);

		/**
		 * Whether to include the given position of any reset operation (eg. the various
		 * seek APIs on the Pulsar consumer).
		 */
		private boolean resetIncludeHead = false;

		/**
		 * Whether pooling of messages and the underlying data buffers is enabled.
		 */
		private boolean poolMessages = false;

		/**
		 * Whether to start the consumer in a paused state.
		 */
		private boolean startPaused = false;

		public ConsumerConfigProperties.Acknowledgement getAck() {
			return this.ack;
		}

		public ConsumerConfigProperties.Chunking getChunk() {
			return this.chunk;
		}

		public ConsumerConfigProperties.Subscription getSubscription() {
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

		public int getReceiverQueueSize() {
			return this.receiverQueueSize;
		}

		public void setReceiverQueueSize(int receiverQueueSize) {
			this.receiverQueueSize = receiverQueueSize;
		}

		public int getMaxTotalReceiverQueueSizeAcrossPartitions() {
			return this.maxTotalReceiverQueueSizeAcrossPartitions;
		}

		public void setMaxTotalReceiverQueueSizeAcrossPartitions(int maxTotalReceiverQueueSizeAcrossPartitions) {
			this.maxTotalReceiverQueueSizeAcrossPartitions = maxTotalReceiverQueueSizeAcrossPartitions;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getPriorityLevel() {
			return this.priorityLevel;
		}

		public void setPriorityLevel(int priorityLevel) {
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

		public boolean getReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public int getPatternAutoDiscoveryPeriod() {
			return this.patternAutoDiscoveryPeriod;
		}

		public void setPatternAutoDiscoveryPeriod(int patternAutoDiscoveryPeriod) {
			this.patternAutoDiscoveryPeriod = patternAutoDiscoveryPeriod;
		}

		public DeadLetterPolicyConfig getDeadLetterPolicy() {
			return this.deadLetterPolicy;
		}

		public void setDeadLetterPolicy(DeadLetterPolicyConfig deadLetterPolicy) {
			this.deadLetterPolicy = deadLetterPolicy;
		}

		public boolean getRetryEnable() {
			return this.retryEnable;
		}

		public void setRetryEnable(boolean retryEnable) {
			this.retryEnable = retryEnable;
		}

		public boolean getAutoUpdatePartitions() {
			return this.autoUpdatePartitions;
		}

		public void setAutoUpdatePartitions(boolean autoUpdatePartitions) {
			this.autoUpdatePartitions = autoUpdatePartitions;
		}

		public Duration getAutoUpdatePartitionsInterval() {
			return this.autoUpdatePartitionsInterval;
		}

		public void setAutoUpdatePartitionsInterval(Duration autoUpdatePartitionsInterval) {
			this.autoUpdatePartitionsInterval = autoUpdatePartitionsInterval;
		}

		public boolean getResetIncludeHead() {
			return this.resetIncludeHead;
		}

		public void setResetIncludeHead(boolean resetIncludeHead) {
			this.resetIncludeHead = resetIncludeHead;
		}

		public boolean getPoolMessages() {
			return this.poolMessages;
		}

		public void setPoolMessages(boolean poolMessages) {
			this.poolMessages = poolMessages;
		}

		public boolean getStartPaused() {
			return this.startPaused;
		}

		public void setStartPaused(boolean startPaused) {
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

		private void mapAcknowledgementProperties(ConsumerConfigProperties.Acknowledgement ack, PropertyMapper map,
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

		private void mapChunkingProperties(ConsumerConfigProperties.Chunking chunk, PropertyMapper map,
				ConsumerBuilder<?> consumerBuilder) {
			map.from(chunk::getAutoAckOldestOnQueueFull).to(consumerBuilder::autoAckOldestChunkedMessageOnQueueFull);
			map.from(chunk::getExpireTimeIncomplete)
				.as(Duration::toMillis)
				.to(consumerBuilder, (cb, val) -> cb.expireTimeOfIncompleteChunkedMessage(val, TimeUnit.MILLISECONDS));
			map.from(chunk::getMaxPendingMessages).to(consumerBuilder::maxPendingChunkedMessage);
		}

		private void mapSubscriptionProperties(ConsumerConfigProperties.Subscription subscription, PropertyMapper map,
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
			 * Whether to automatically drop outstanding uncompleted chunked messages once
			 * the consumer queue reaches the threshold set by the 'maxPendingMessages'
			 * property.
			 */
			private Boolean autoAckOldestOnQueueFull = true;

			/**
			 * The maximum time period for a consumer to receive all chunks of a message -
			 * if this threshold is exceeded the consumer will expire the incomplete
			 * chunks.
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

	public static class ProducerConfigProperties {

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
		 * Whether the "send" and "sendAsync" methods should block if the outgoing message
		 * queue is full.
		 */
		private boolean blockIfQueueFull;

		/**
		 * Maximum number of pending messages for the producer.
		 */
		private int maxPendingMessages = 1000;

		/**
		 * Maximum number of pending messages across all the partitions.
		 */
		private int maxPendingMessagesAcrossPartitions = 50000;

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
		private boolean chunkingEnabled;

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
		private boolean autoUpdatePartitions = true;

		/**
		 * Interval of partitions discovery updates.
		 */
		private Duration autoUpdatePartitionsInterval = Duration.ofMinutes(1);

		/**
		 * Whether the multiple schema mode is enabled.
		 */
		private boolean multiSchema = true;

		/**
		 * Type of access to the topic the producer requires.
		 */
		private ProducerAccessMode accessMode = ProducerAccessMode.Shared;

		/**
		 * Whether producers in Shared mode register and connect immediately to the owner
		 * broker of each partition or start lazily on demand.
		 */
		private boolean lazyStartPartitionedProducers = false;

		/**
		 * Map of properties to add to the producer.
		 */
		private Map<String, String> properties = new HashMap<>();

		private final PulsarProperties.Cache cache = new PulsarProperties.Cache();

		public Batching getBatch() {
			return this.batch;
		}

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

		public boolean getBlockIfQueueFull() {
			return this.blockIfQueueFull;
		}

		public void setBlockIfQueueFull(boolean blockIfQueueFull) {
			this.blockIfQueueFull = blockIfQueueFull;
		}

		public int getMaxPendingMessages() {
			return this.maxPendingMessages;
		}

		public void setMaxPendingMessages(int maxPendingMessages) {
			this.maxPendingMessages = maxPendingMessages;
		}

		public int getMaxPendingMessagesAcrossPartitions() {
			return this.maxPendingMessagesAcrossPartitions;
		}

		public void setMaxPendingMessagesAcrossPartitions(int maxPendingMessagesAcrossPartitions) {
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

		public boolean getChunkingEnabled() {
			return this.chunkingEnabled;
		}

		public void setChunkingEnabled(boolean chunkingEnabled) {
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

		public boolean getAutoUpdatePartitions() {
			return this.autoUpdatePartitions;
		}

		public void setAutoUpdatePartitions(boolean autoUpdatePartitions) {
			this.autoUpdatePartitions = autoUpdatePartitions;
		}

		public Duration getAutoUpdatePartitionsInterval() {
			return this.autoUpdatePartitionsInterval;
		}

		public void setAutoUpdatePartitionsInterval(Duration autoUpdatePartitionsInterval) {
			this.autoUpdatePartitionsInterval = autoUpdatePartitionsInterval;
		}

		public boolean getMultiSchema() {
			return this.multiSchema;
		}

		public void setMultiSchema(boolean multiSchema) {
			this.multiSchema = multiSchema;
		}

		public ProducerAccessMode getAccessMode() {
			return this.accessMode;
		}

		public void setAccessMode(ProducerAccessMode accessMode) {
			this.accessMode = accessMode;
		}

		public boolean getLazyStartPartitionedProducers() {
			return this.lazyStartPartitionedProducers;
		}

		public void setLazyStartPartitionedProducers(boolean lazyStartPartitionedProducers) {
			this.lazyStartPartitionedProducers = lazyStartPartitionedProducers;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}

		public PulsarProperties.Cache getCache() {
			return this.cache;
		}

		@SuppressWarnings("deprecation")
		public ProducerBuilderCustomizer<?> toProducerBuilderCustomizer() {
			return (producerBuilder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getTopicName).to(producerBuilder::topic);
				map.from(this::getName).to(producerBuilder::producerName);
				map.from(this::getSendTimeout)
					.asInt(Duration::toMillis)
					.to(producerBuilder, (pb, val) -> pb.sendTimeout(val, TimeUnit.MILLISECONDS));
				map.from(this::getBlockIfQueueFull).to(producerBuilder::blockIfQueueFull);
				map.from(this::getMaxPendingMessages).to(producerBuilder::maxPendingMessages);
				map.from(this::getMaxPendingMessagesAcrossPartitions)
					.to(producerBuilder::maxPendingMessagesAcrossPartitions);
				map.from(this::getMessageRoutingMode).to(producerBuilder::messageRoutingMode);
				map.from(this::getHashingScheme).to(producerBuilder::hashingScheme);
				map.from(this::getCryptoFailureAction).to(producerBuilder::cryptoFailureAction);
				map.from(this::getBatch)
					.as(Batching::getMaxPublishDelay)
					.as(Duration::toMillis)
					.to(producerBuilder, (pb, val) -> pb.batchingMaxPublishDelay(val, TimeUnit.MILLISECONDS));
				map.from(this::getBatch)
					.as(Batching::getPartitionSwitchFrequencyByPublishDelay)
					.to(producerBuilder::roundRobinRouterBatchingPartitionSwitchFrequency);
				map.from(this::getBatch).as(Batching::getMaxMessages).to(producerBuilder::batchingMaxMessages);
				map.from(this::getBatch)
					.as(Batching::getMaxBytes)
					.asInt(DataSize::toBytes)
					.to(producerBuilder::batchingMaxBytes);
				map.from(this::getBatch).as(Batching::getEnabled).to(producerBuilder::enableBatching);
				map.from(this::getChunkingEnabled).to(producerBuilder::enableChunking);
				map.from(this::getEncryptionKeys)
					.to((encryptionKeys) -> encryptionKeys.forEach(producerBuilder::addEncryptionKey));
				map.from(this::getCompressionType).to(producerBuilder::compressionType);
				map.from(this::getInitialSequenceId).to(producerBuilder::initialSequenceId);
				map.from(this::getAutoUpdatePartitions).to(producerBuilder::autoUpdatePartitions);
				map.from(this::getAutoUpdatePartitionsInterval)
					.asInt(Duration::toMillis)
					.to(producerBuilder, (pb, val) -> pb.autoUpdatePartitionsInterval(val, TimeUnit.MILLISECONDS));
				map.from(this::getMultiSchema).to(producerBuilder::enableMultiSchema);
				map.from(this::getAccessMode).to(producerBuilder::accessMode);
				map.from(this::getLazyStartPartitionedProducers)
					.to(producerBuilder::enableLazyStartPartitionedProducers);
				map.from(this::getProperties).to(producerBuilder::properties);
			};
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
			private Integer partitionSwitchFrequencyByPublishDelay = 10;

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

			public Integer getPartitionSwitchFrequencyByPublishDelay() {
				return this.partitionSwitchFrequencyByPublishDelay;
			}

			public void setPartitionSwitchFrequencyByPublishDelay(Integer partitionSwitchFrequencyByPublishDelay) {
				this.partitionSwitchFrequencyByPublishDelay = partitionSwitchFrequencyByPublishDelay;
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

	}

	public static class Client {

		/**
		 * Pulsar service URL in the format
		 * '(pulsar|pulsar+ssl)://&lt;host&gt;:&lt;port&gt;'.
		 */
		private String serviceUrl = "pulsar://localhost:6650";

		/**
		 * Listener name for lookup. Clients can use listenerName to choose one of the
		 * listeners as the service URL to create a connection to the broker. To use this,
		 * "advertisedListeners" must be enabled on the broker.
		 */
		private String listenerName;

		/**
		 * Fully qualified class name of the authentication plugin.
		 */
		private String authPluginClassName;

		/**
		 * Authentication parameter(s) as a JSON encoded string.
		 */
		private String authParams;

		/**
		 * Authentication parameter(s) as a map of parameter names to parameter values.
		 */
		private Map<String, String> authentication;

		/**
		 * Client operation timeout.
		 */
		private Duration operationTimeout = Duration.ofSeconds(30);

		/**
		 * Client lookup timeout.
		 */
		private Duration lookupTimeout = Duration.ofMillis(-1);

		/**
		 * Number of threads to be used for handling connections to brokers.
		 */
		private int numIoThreads = 1;

		/**
		 * Number of threads to be used for message listeners. The listener thread pool is
		 * shared across all the consumers and readers that are using a "listener" model
		 * to get messages. For a given consumer, the listener will always be invoked from
		 * the same thread, to ensure ordering.
		 */
		private int numListenerThreads = 1;

		/**
		 * Maximum number of connections that the client will open to a single broker.
		 */
		private int numConnectionsPerBroker = 1;

		/**
		 * Whether to use TCP no-delay flag on the connection, to disable Nagle algorithm.
		 */
		private boolean useTcpNoDelay = true;

		/**
		 * Whether to use TLS encryption on the connection.
		 */
		private boolean useTls = false;

		/**
		 * Whether the hostname is validated when the proxy creates a TLS connection with
		 * brokers.
		 */
		private boolean tlsHostnameVerificationEnable = false;

		/**
		 * Path to the trusted TLS certificate file.
		 */
		private String tlsTrustCertsFilePath;

		/**
		 * Path to the TLS certificate file.
		 */
		private String tlsCertificateFilePath;

		/**
		 * Path to the TLS private key file.
		 */
		private String tlsKeyFilePath;

		/**
		 * Whether the client accepts untrusted TLS certificates from the broker.
		 */
		private Boolean tlsAllowInsecureConnection = false;

		/**
		 * Enable KeyStore instead of PEM type configuration if TLS is enabled.
		 */
		private boolean useKeyStoreTls = false;

		/**
		 * Name of the security provider used for SSL connections.
		 */
		private String sslProvider;

		/**
		 * File format of the trust store file.
		 */
		private String tlsTrustStoreType;

		/**
		 * Location of the trust store file.
		 */
		private String tlsTrustStorePath;

		/**
		 * Store password for the key store file.
		 */
		private String tlsTrustStorePassword;

		/**
		 * Comma-separated list of cipher suites. This is a named combination of
		 * authentication, encryption, MAC and key exchange algorithm used to negotiate
		 * the security settings for a network connection using TLS or SSL network
		 * protocol. By default, all the available cipher suites are supported.
		 */
		private Set<String> tlsCiphers;

		/**
		 * Comma-separated list of SSL protocols used to generate the SSLContext. Allowed
		 * values in recent JVMs are TLS, TLSv1.3, TLSv1.2 and TLSv1.1.
		 */
		private Set<String> tlsProtocols;

		/**
		 * Interval between each stat info.
		 */
		private Duration statsInterval = Duration.ofSeconds(60);

		/**
		 * Number of concurrent lookup-requests allowed to send on each broker-connection
		 * to prevent overload on broker.
		 */
		private int maxConcurrentLookupRequest = 5000;

		/**
		 * Number of max lookup-requests allowed on each broker-connection to prevent
		 * overload on broker.
		 */
		private int maxLookupRequest = 50000;

		/**
		 * Maximum number of times a lookup-request to a broker will be redirected.
		 */
		private int maxLookupRedirects = 20;

		/**
		 * Maximum number of broker-rejected requests in a certain timeframe, after which
		 * the current connection is closed and a new connection is created by the client.
		 */
		private int maxNumberOfRejectedRequestPerConnection = 50;

		/**
		 * Keep alive interval for broker-client connection.
		 */
		private Duration keepAliveInterval = Duration.ofSeconds(30);

		/**
		 * Duration to wait for a connection to a broker to be established.
		 */
		private Duration connectionTimeout = Duration.ofSeconds(10);

		/**
		 * Maximum duration for completing a request.
		 */
		private Duration requestTimeout = Duration.ofMinutes(1);

		/**
		 * Initial backoff interval.
		 */
		private Duration initialBackoffInterval = Duration.ofMillis(100);

		/**
		 * Maximum backoff interval.
		 */
		private Duration maxBackoffInterval = Duration.ofSeconds(30);

		/**
		 * Enables spin-waiting on executors and IO threads in order to reduce latency
		 * during context switches.
		 */
		private boolean enableBusyWait = false;

		/**
		 * Limit of direct memory that will be allocated by the client.
		 */
		private DataSize memoryLimit = DataSize.ofMegabytes(64);

		/**
		 * URL of proxy service. proxyServiceUrl and proxyProtocol must be mutually
		 * inclusive.
		 */
		private String proxyServiceUrl;

		/**
		 * Enables transactions. To use this, start the transactionCoordinatorClient with
		 * the pulsar client.
		 */
		private boolean enableTransaction = false;

		/**
		 * DNS lookup bind address.
		 */
		private String dnsLookupBindAddress;

		/**
		 * DNS lookup bind port.
		 */
		private int dnsLookupBindPort = 0;

		/**
		 * SOCKS5 proxy address.
		 */
		private String socks5ProxyAddress;

		/**
		 * SOCKS5 proxy username.
		 */
		private String socks5ProxyUsername;

		/**
		 * SOCKS5 proxy password.
		 */
		private String socks5ProxyPassword;

		public String getServiceUrl() {
			return this.serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}

		public String getListenerName() {
			return this.listenerName;
		}

		public void setListenerName(String listenerName) {
			this.listenerName = listenerName;
		}

		public String getAuthPluginClassName() {
			return this.authPluginClassName;
		}

		public void setAuthPluginClassName(String authPluginClassName) {
			this.authPluginClassName = authPluginClassName;
		}

		public String getAuthParams() {
			return this.authParams;
		}

		public void setAuthParams(String authParams) {
			this.authParams = authParams;
		}

		public Map<String, String> getAuthentication() {
			return this.authentication;
		}

		public void setAuthentication(Map<String, String> authentication) {
			this.authentication = authentication;
		}

		public Duration getOperationTimeout() {
			return this.operationTimeout;
		}

		public void setOperationTimeout(Duration operationTimeout) {
			this.operationTimeout = operationTimeout;
		}

		public Duration getLookupTimeout() {
			return this.lookupTimeout;
		}

		public void setLookupTimeout(Duration lookupTimeout) {
			this.lookupTimeout = lookupTimeout;
		}

		public int getNumIoThreads() {
			return this.numIoThreads;
		}

		public void setNumIoThreads(int numIoThreads) {
			this.numIoThreads = numIoThreads;
		}

		public int getNumListenerThreads() {
			return this.numListenerThreads;
		}

		public void setNumListenerThreads(int numListenerThreads) {
			this.numListenerThreads = numListenerThreads;
		}

		public int getNumConnectionsPerBroker() {
			return this.numConnectionsPerBroker;
		}

		public void setNumConnectionsPerBroker(int numConnectionsPerBroker) {
			this.numConnectionsPerBroker = numConnectionsPerBroker;
		}

		public boolean getUseTcpNoDelay() {
			return this.useTcpNoDelay;
		}

		public void setUseTcpNoDelay(boolean useTcpNoDelay) {
			this.useTcpNoDelay = useTcpNoDelay;
		}

		public boolean getUseTls() {
			return this.useTls;
		}

		public void setUseTls(boolean useTls) {
			this.useTls = useTls;
		}

		public boolean getTlsHostnameVerificationEnable() {
			return this.tlsHostnameVerificationEnable;
		}

		public void setTlsHostnameVerificationEnable(boolean tlsHostnameVerificationEnable) {
			this.tlsHostnameVerificationEnable = tlsHostnameVerificationEnable;
		}

		public String getTlsTrustCertsFilePath() {
			return this.tlsTrustCertsFilePath;
		}

		public void setTlsTrustCertsFilePath(String tlsTrustCertsFilePath) {
			this.tlsTrustCertsFilePath = tlsTrustCertsFilePath;
		}

		public String getTlsCertificateFilePath() {
			return this.tlsCertificateFilePath;
		}

		public void setTlsCertificateFilePath(String tlsCertificateFilePath) {
			this.tlsCertificateFilePath = tlsCertificateFilePath;
		}

		public String getTlsKeyFilePath() {
			return this.tlsKeyFilePath;
		}

		public void setTlsKeyFilePath(String tlsKeyFilePath) {
			this.tlsKeyFilePath = tlsKeyFilePath;
		}

		public Boolean getTlsAllowInsecureConnection() {
			return this.tlsAllowInsecureConnection;
		}

		public void setTlsAllowInsecureConnection(Boolean tlsAllowInsecureConnection) {
			this.tlsAllowInsecureConnection = tlsAllowInsecureConnection;
		}

		public boolean getUseKeyStoreTls() {
			return this.useKeyStoreTls;
		}

		public void setUseKeyStoreTls(boolean useKeyStoreTls) {
			this.useKeyStoreTls = useKeyStoreTls;
		}

		public String getSslProvider() {
			return this.sslProvider;
		}

		public void setSslProvider(String sslProvider) {
			this.sslProvider = sslProvider;
		}

		public String getTlsTrustStoreType() {
			return this.tlsTrustStoreType;
		}

		public void setTlsTrustStoreType(String tlsTrustStoreType) {
			this.tlsTrustStoreType = tlsTrustStoreType;
		}

		public String getTlsTrustStorePath() {
			return this.tlsTrustStorePath;
		}

		public void setTlsTrustStorePath(String tlsTrustStorePath) {
			this.tlsTrustStorePath = tlsTrustStorePath;
		}

		public String getTlsTrustStorePassword() {
			return this.tlsTrustStorePassword;
		}

		public void setTlsTrustStorePassword(String tlsTrustStorePassword) {
			this.tlsTrustStorePassword = tlsTrustStorePassword;
		}

		public Set<String> getTlsCiphers() {
			return this.tlsCiphers;
		}

		public void setTlsCiphers(Set<String> tlsCiphers) {
			this.tlsCiphers = tlsCiphers;
		}

		public Set<String> getTlsProtocols() {
			return this.tlsProtocols;
		}

		public void setTlsProtocols(Set<String> tlsProtocols) {
			this.tlsProtocols = tlsProtocols;
		}

		public Duration getStatsInterval() {
			return this.statsInterval;
		}

		public void setStatsInterval(Duration statsInterval) {
			this.statsInterval = statsInterval;
		}

		public int getMaxConcurrentLookupRequest() {
			return this.maxConcurrentLookupRequest;
		}

		public void setMaxConcurrentLookupRequest(int maxConcurrentLookupRequest) {
			this.maxConcurrentLookupRequest = maxConcurrentLookupRequest;
		}

		public int getMaxLookupRequest() {
			return this.maxLookupRequest;
		}

		public void setMaxLookupRequest(int maxLookupRequest) {
			this.maxLookupRequest = maxLookupRequest;
		}

		public int getMaxLookupRedirects() {
			return this.maxLookupRedirects;
		}

		public void setMaxLookupRedirects(int maxLookupRedirects) {
			this.maxLookupRedirects = maxLookupRedirects;
		}

		public int getMaxNumberOfRejectedRequestPerConnection() {
			return this.maxNumberOfRejectedRequestPerConnection;
		}

		public void setMaxNumberOfRejectedRequestPerConnection(int maxNumberOfRejectedRequestPerConnection) {
			this.maxNumberOfRejectedRequestPerConnection = maxNumberOfRejectedRequestPerConnection;
		}

		public Duration getKeepAliveInterval() {
			return this.keepAliveInterval;
		}

		public void setKeepAliveInterval(Duration keepAliveInterval) {
			this.keepAliveInterval = keepAliveInterval;
		}

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

		public Duration getInitialBackoffInterval() {
			return this.initialBackoffInterval;
		}

		public void setInitialBackoffInterval(Duration initialBackoffInterval) {
			this.initialBackoffInterval = initialBackoffInterval;
		}

		public Duration getMaxBackoffInterval() {
			return this.maxBackoffInterval;
		}

		public void setMaxBackoffInterval(Duration maxBackoffInterval) {
			this.maxBackoffInterval = maxBackoffInterval;
		}

		public boolean getEnableBusyWait() {
			return this.enableBusyWait;
		}

		public void setEnableBusyWait(boolean enableBusyWait) {
			this.enableBusyWait = enableBusyWait;
		}

		public DataSize getMemoryLimit() {
			return this.memoryLimit;
		}

		public void setMemoryLimit(DataSize memoryLimit) {
			this.memoryLimit = memoryLimit;
		}

		public String getProxyServiceUrl() {
			return this.proxyServiceUrl;
		}

		public void setProxyServiceUrl(String proxyServiceUrl) {
			this.proxyServiceUrl = proxyServiceUrl;
		}

		public boolean getEnableTransaction() {
			return this.enableTransaction;
		}

		public void setEnableTransaction(boolean enableTransaction) {
			this.enableTransaction = enableTransaction;
		}

		public String getDnsLookupBindAddress() {
			return this.dnsLookupBindAddress;
		}

		public void setDnsLookupBindAddress(String dnsLookupBindAddress) {
			this.dnsLookupBindAddress = dnsLookupBindAddress;
		}

		public int getDnsLookupBindPort() {
			return this.dnsLookupBindPort;
		}

		public void setDnsLookupBindPort(int dnsLookupBindPort) {
			this.dnsLookupBindPort = dnsLookupBindPort;
		}

		public String getSocks5ProxyAddress() {
			return this.socks5ProxyAddress;
		}

		public void setSocks5ProxyAddress(String socks5ProxyAddress) {
			this.socks5ProxyAddress = socks5ProxyAddress;
		}

		public String getSocks5ProxyUsername() {
			return this.socks5ProxyUsername;
		}

		public void setSocks5ProxyUsername(String socks5ProxyUsername) {
			this.socks5ProxyUsername = socks5ProxyUsername;
		}

		public String getSocks5ProxyPassword() {
			return this.socks5ProxyPassword;
		}

		public void setSocks5ProxyPassword(String socks5ProxyPassword) {
			this.socks5ProxyPassword = socks5ProxyPassword;
		}

	}

	public static class Function {

		/**
		 * Whether to stop processing further function creates/updates when a failure
		 * occurs.
		 */
		private boolean failFast = true;

		/**
		 * Whether to throw an exception if any failure is encountered during server
		 * startup while creating/updating functions.
		 */
		private boolean propagateFailures = true;

		/**
		 * Whether to throw an exception if any failure is encountered during server
		 * shutdown while enforcing stop policy on functions.
		 */
		private boolean propagateStopFailures = false;

		public boolean getFailFast() {
			return this.failFast;
		}

		public void setFailFast(boolean failFast) {
			this.failFast = failFast;
		}

		public boolean getPropagateFailures() {
			return this.propagateFailures;
		}

		public void setPropagateFailures(boolean propagateFailures) {
			this.propagateFailures = propagateFailures;
		}

		public boolean getPropagateStopFailures() {
			return this.propagateStopFailures;
		}

		public void setPropagateStopFailures(boolean propagateStopFailures) {
			this.propagateStopFailures = propagateStopFailures;
		}

	}

	public static class Listener {

		/**
		 * AckMode for acknowledgements. Allowed values are RECORD, BATCH, MANUAL.
		 */
		private AckMode ackMode;

		/**
		 * SchemaType of the consumed messages.
		 */
		private SchemaType schemaType;

		/**
		 * Max number of messages in a single batch request.
		 */
		private int maxNumMessages = -1;

		/**
		 * Max size in a single batch request.
		 */
		private DataSize maxNumBytes = DataSize.ofMegabytes(10);

		/**
		 * Duration to wait for enough message to fill a batch request before timing out.
		 */
		private Duration batchTimeout = Duration.ofMillis(100);

		/**
		 * Whether to record observations for receive operations when the Observations API
		 * is available.
		 */
		private Boolean observationsEnabled = true;

		public AckMode getAckMode() {
			return this.ackMode;
		}

		public void setAckMode(AckMode ackMode) {
			this.ackMode = ackMode;
		}

		public SchemaType getSchemaType() {
			return this.schemaType;
		}

		public void setSchemaType(SchemaType schemaType) {
			this.schemaType = schemaType;
		}

		public int getMaxNumMessages() {
			return this.maxNumMessages;
		}

		public void setMaxNumMessages(int maxNumMessages) {
			this.maxNumMessages = maxNumMessages;
		}

		public DataSize getMaxNumBytes() {
			return this.maxNumBytes;
		}

		public void setMaxNumBytes(DataSize maxNumBytes) {
			this.maxNumBytes = maxNumBytes;
		}

		public Duration getBatchTimeout() {
			return this.batchTimeout;
		}

		public void setBatchTimeout(Duration batchTimeout) {
			this.batchTimeout = batchTimeout;
		}

		public Boolean isObservationsEnabled() {
			return this.observationsEnabled;
		}

		public void setObservationsEnabled(Boolean observationsEnabled) {
			this.observationsEnabled = observationsEnabled;
		}

	}

	public static class Admin {

		/**
		 * Pulsar web URL for the admin endpoint in the format
		 * '(http|https)://&lt;host&gt;:&lt;port&gt;'.
		 */
		private String serviceUrl = "http://localhost:8080";

		/**
		 * Fully qualified class name of the authentication plugin.
		 */
		private String authPluginClassName;

		/**
		 * Authentication parameter(s) as a JSON encoded string.
		 */
		private String authParams;

		/**
		 * Authentication parameter(s) as a map of parameter names to parameter values.
		 */
		private Map<String, String> authentication;

		/**
		 * Path to the trusted TLS certificate file.
		 */
		private String tlsTrustCertsFilePath;

		/**
		 * Path to the TLS certificate file.
		 */
		private String tlsCertificateFilePath;

		/**
		 * Path to the TLS private key file.
		 */
		private String tlsKeyFilePath;

		/**
		 * Whether the client accepts untrusted TLS certificates from the broker.
		 */
		private boolean tlsAllowInsecureConnection = false;

		/**
		 * Whether the hostname is validated when the proxy creates a TLS connection with
		 * brokers.
		 */
		private boolean tlsHostnameVerificationEnable = false;

		/**
		 * Enable KeyStore instead of PEM type configuration if TLS is enabled.
		 */
		private boolean useKeyStoreTls = false;

		/**
		 * Name of the security provider used for SSL connections.
		 */
		private String sslProvider;

		/**
		 * File format of the trust store file.
		 */
		private String tlsTrustStoreType;

		/**
		 * Location of the trust store file.
		 */
		private String tlsTrustStorePath;

		/**
		 * Store password for the key store file.
		 */
		private String tlsTrustStorePassword;

		/**
		 * List of cipher suites. This is a named combination of authentication,
		 * encryption, MAC and key exchange algorithm used to negotiate the security
		 * settings for a network connection using TLS or SSL network protocol. By
		 * default, all the available cipher suites are supported.
		 */
		private Set<String> tlsCiphers;

		/**
		 * List of SSL protocols used to generate the SSLContext. Allowed values in recent
		 * JVMs are TLS, TLSv1.3, TLSv1.2 and TLSv1.1.
		 */
		private Set<String> tlsProtocols;

		/**
		 * Duration to wait for a connection to server to be established.
		 */
		private Duration connectionTimeout = Duration.ofMinutes(1);

		/**
		 * Server response read time out for any request.
		 */
		private Duration readTimeout = Duration.ofMinutes(1);

		/**
		 * Server request time out for any request.
		 */
		private Duration requestTimeout = Duration.ofMinutes(5);

		/**
		 * Certificates auto refresh time if Pulsar admin uses tls authentication.
		 */
		private Duration autoCertRefreshTime = Duration.ofMinutes(5);

		public String getServiceUrl() {
			return this.serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}

		public String getAuthPluginClassName() {
			return this.authPluginClassName;
		}

		public void setAuthPluginClassName(String authPluginClassName) {
			this.authPluginClassName = authPluginClassName;
		}

		public String getAuthParams() {
			return this.authParams;
		}

		public void setAuthParams(String authParams) {
			this.authParams = authParams;
		}

		public Map<String, String> getAuthentication() {
			return this.authentication;
		}

		public void setAuthentication(Map<String, String> authentication) {
			this.authentication = authentication;
		}

		public String getTlsTrustCertsFilePath() {
			return this.tlsTrustCertsFilePath;
		}

		public void setTlsTrustCertsFilePath(String tlsTrustCertsFilePath) {
			this.tlsTrustCertsFilePath = tlsTrustCertsFilePath;
		}

		public String getTlsCertificateFilePath() {
			return this.tlsCertificateFilePath;
		}

		public void setTlsCertificateFilePath(String tlsCertificateFilePath) {
			this.tlsCertificateFilePath = tlsCertificateFilePath;
		}

		public String getTlsKeyFilePath() {
			return this.tlsKeyFilePath;
		}

		public void setTlsKeyFilePath(String tlsKeyFilePath) {
			this.tlsKeyFilePath = tlsKeyFilePath;
		}

		public Boolean isTlsAllowInsecureConnection() {
			return this.tlsAllowInsecureConnection;
		}

		public void setTlsAllowInsecureConnection(boolean tlsAllowInsecureConnection) {
			this.tlsAllowInsecureConnection = tlsAllowInsecureConnection;
		}

		public Boolean isTlsHostnameVerificationEnable() {
			return this.tlsHostnameVerificationEnable;
		}

		public void setTlsHostnameVerificationEnable(boolean tlsHostnameVerificationEnable) {
			this.tlsHostnameVerificationEnable = tlsHostnameVerificationEnable;
		}

		public Boolean isUseKeyStoreTls() {
			return this.useKeyStoreTls;
		}

		public void setUseKeyStoreTls(boolean useKeyStoreTls) {
			this.useKeyStoreTls = useKeyStoreTls;
		}

		public String getSslProvider() {
			return this.sslProvider;
		}

		public void setSslProvider(String sslProvider) {
			this.sslProvider = sslProvider;
		}

		public String getTlsTrustStoreType() {
			return this.tlsTrustStoreType;
		}

		public void setTlsTrustStoreType(String tlsTrustStoreType) {
			this.tlsTrustStoreType = tlsTrustStoreType;
		}

		public String getTlsTrustStorePath() {
			return this.tlsTrustStorePath;
		}

		public void setTlsTrustStorePath(String tlsTrustStorePath) {
			this.tlsTrustStorePath = tlsTrustStorePath;
		}

		public String getTlsTrustStorePassword() {
			return this.tlsTrustStorePassword;
		}

		public void setTlsTrustStorePassword(String tlsTrustStorePassword) {
			this.tlsTrustStorePassword = tlsTrustStorePassword;
		}

		public Set<String> getTlsCiphers() {
			return this.tlsCiphers;
		}

		public void setTlsCiphers(Set<String> tlsCiphers) {
			this.tlsCiphers = tlsCiphers;
		}

		public Set<String> getTlsProtocols() {
			return this.tlsProtocols;
		}

		public void setTlsProtocols(Set<String> tlsProtocols) {
			this.tlsProtocols = tlsProtocols;
		}

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Duration getReadTimeout() {
			return this.readTimeout;
		}

		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

		public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

		public Duration getAutoCertRefreshTime() {
			return this.autoCertRefreshTime;
		}

		public void setAutoCertRefreshTime(Duration autoCertRefreshTime) {
			this.autoCertRefreshTime = autoCertRefreshTime;
		}

		public PulsarAdminBuilderCustomizer toPulsarAdminBuilderCustomizer() {
			return (adminBuilder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getServiceUrl).to(adminBuilder::serviceHttpUrl);
				applyAuthentication(adminBuilder);
				map.from(this::getTlsTrustCertsFilePath).to(adminBuilder::tlsTrustCertsFilePath);
				map.from(this::getTlsCertificateFilePath).to(adminBuilder::tlsCertificateFilePath);
				map.from(this::getTlsKeyFilePath).to(adminBuilder::tlsKeyFilePath);
				map.from(this::isTlsAllowInsecureConnection).to(adminBuilder::allowTlsInsecureConnection);
				map.from(this::isTlsHostnameVerificationEnable).to(adminBuilder::enableTlsHostnameVerification);
				map.from(this::isUseKeyStoreTls).to(adminBuilder::useKeyStoreTls);
				map.from(this::getSslProvider).to(adminBuilder::sslProvider);
				map.from(this::getTlsTrustStoreType).to(adminBuilder::tlsTrustStoreType);
				map.from(this::getTlsTrustStorePath).to(adminBuilder::tlsTrustStorePath);
				map.from(this::getTlsTrustStorePassword).to(adminBuilder::tlsTrustStorePassword);
				map.from(this::getTlsCiphers).to(adminBuilder::tlsCiphers);
				map.from(this::getTlsProtocols).to(adminBuilder::tlsProtocols);
				map.from(this::getConnectionTimeout)
					.asInt(Duration::toMillis)
					.to(adminBuilder, (ab, val) -> ab.connectionTimeout(val, TimeUnit.MILLISECONDS));
				map.from(this::getReadTimeout)
					.asInt(Duration::toMillis)
					.to(adminBuilder, (ab, val) -> ab.readTimeout(val, TimeUnit.MILLISECONDS));
				map.from(this::getRequestTimeout)
					.asInt(Duration::toMillis)
					.to(adminBuilder, (ab, val) -> ab.requestTimeout(val, TimeUnit.MILLISECONDS));
				map.from(this::getAutoCertRefreshTime)
					.asInt(Duration::toMillis)
					.to(adminBuilder, (ab, val) -> ab.autoCertRefreshTime(val, TimeUnit.MILLISECONDS));
			};
		}

		private void applyAuthentication(PulsarAdminBuilder adminBuilder) {
			if (StringUtils.hasText(this.getAuthParams()) && !CollectionUtils.isEmpty(this.getAuthentication())) {
				throw new IllegalArgumentException(
						"Cannot set both spring.pulsar.administration.authParams and spring.pulsar.administration.authentication.*");
			}
			var authPluginClass = this.getAuthPluginClassName();
			if (StringUtils.hasText(authPluginClass)) {
				var authParams = this.getAuthParams();
				if (this.getAuthentication() != null) {
					authParams = AuthParameterUtils.maybeConvertToEncodedParamString(this.getAuthentication());
				}
				try {
					adminBuilder.authentication(authPluginClass, authParams);
				}
				catch (PulsarClientException.UnsupportedAuthenticationException ex) {
					throw new IllegalArgumentException("Unable to configure authentication: " + ex.getMessage(), ex);
				}
			}
		}

	}

	public static class Reader {

		/**
		 * Topic names.
		 */
		private List<String> topicNames;

		/**
		 * Size of a consumer's receiver queue.
		 */
		private Integer receiverQueueSize;

		/**
		 * Reader name.
		 */
		private String name;

		/**
		 * Subscription name.
		 */
		private String subscriptionName;

		/**
		 * Prefix of subscription role.
		 */
		private String subscriptionRolePrefix;

		/**
		 * Whether to read messages from a compacted topic rather than a full message
		 * backlog of a topic.
		 */
		private Boolean readCompacted;

		/**
		 * Whether the first message to be returned is the one specified by messageId.
		 */
		private Boolean resetIncludeHead;

		public List<String> getTopicNames() {
			return this.topicNames;
		}

		public void setTopicNames(List<String> topicNames) {
			this.topicNames = topicNames;
		}

		public Integer getReceiverQueueSize() {
			return this.receiverQueueSize;
		}

		public void setReceiverQueueSize(Integer receiverQueueSize) {
			this.receiverQueueSize = receiverQueueSize;
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

		public String getSubscriptionRolePrefix() {
			return this.subscriptionRolePrefix;
		}

		public void setSubscriptionRolePrefix(String subscriptionRolePrefix) {
			this.subscriptionRolePrefix = subscriptionRolePrefix;
		}

		public Boolean getReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(Boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public Boolean getResetIncludeHead() {
			return this.resetIncludeHead;
		}

		public void setResetIncludeHead(Boolean resetIncludeHead) {
			this.resetIncludeHead = resetIncludeHead;
		}

		public ReaderBuilderCustomizer<?> toReaderBuilderCustomizer() {
			return (readerBuilder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getTopicNames).as(ArrayList::new).to(readerBuilder::topics);
				map.from(this::getReceiverQueueSize).to(readerBuilder::receiverQueueSize);
				map.from(this::getName).to(readerBuilder::readerName);
				map.from(this::getSubscriptionName).to(readerBuilder::subscriptionName);
				map.from(this::getSubscriptionRolePrefix).to(readerBuilder::subscriptionRolePrefix);
				map.from(this::getReadCompacted).to(readerBuilder::readCompacted);
				map.from(this::getResetIncludeHead).whenTrue().to((b) -> readerBuilder.startMessageIdInclusive());
			};
		}

	}

	public static class Defaults {

		/**
		 * List of mappings from message type to topic name and schema info to use as a
		 * defaults when a topic name and/or schema is not explicitly specified when
		 * producing or consuming messages of the mapped type.
		 */
		private List<TypeMapping> typeMappings = new ArrayList<>();

		public List<TypeMapping> getTypeMappings() {
			return this.typeMappings;
		}

		public void setTypeMappings(List<TypeMapping> typeMappings) {
			this.typeMappings = typeMappings;
		}

	}

	/**
	 * A mapping from message type to topic and/or schema info to use (at least one of
	 * {@code topicName} or {@code schemaInfo} must be specified.
	 *
	 * @param messageType the message type
	 * @param topicName the topic name
	 * @param schemaInfo the schema info
	 */
	public record TypeMapping(Class<?> messageType, String topicName, SchemaInfo schemaInfo) {
		public TypeMapping {
			Objects.requireNonNull(messageType, "messageType must not be null");
			if (topicName == null && schemaInfo == null) {
				throw new IllegalArgumentException("At least one of topicName or schemaInfo must not be null");
			}
		}
	}

	/**
	 * Represents a schema - holds enough information to construct an actual schema
	 * instance.
	 *
	 * @param schemaType schema type
	 * @param messageKeyType message key type (required for key value type)
	 */
	public record SchemaInfo(SchemaType schemaType, Class<?> messageKeyType) {
		public SchemaInfo {
			Objects.requireNonNull(schemaType, "schemaType must not be null");
			if (schemaType == SchemaType.NONE) {
				throw new IllegalArgumentException("schemaType NONE not supported");
			}
			if (schemaType != SchemaType.KEY_VALUE && messageKeyType != null) {
				throw new IllegalArgumentException("messageKeyType can only be set when schemaType is KEY_VALUE");
			}
		}
	}

}
