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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;

/**
 * Configuration properties Apache Pulsar.
 *
 * @author Chris Bono
 * @author Phillip Webb
 * @since 3.2.0
 */
@ConfigurationProperties("spring.pulsar")
public class PulsarProperties {

	private final Client client = new Client();

	private final Admin admin = new Admin();

	private final Defaults defaults = new Defaults();

	private final Function function = new Function();

	private final Producer producer = new Producer();

	private final Consumer consumer = new Consumer();

	private final Listener listener = new Listener();

	private final Reader reader = new Reader();

	private final Template template = new Template();

	public Client getClient() {
		return this.client;
	}

	public Admin getAdmin() {
		return this.admin;
	}

	public Defaults getDefaults() {
		return this.defaults;
	}

	public Producer getProducer() {
		return this.producer;
	}

	public Consumer getConsumer() {
		return this.consumer;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Reader getReader() {
		return this.reader;
	}

	public Function getFunction() {
		return this.function;
	}

	public Template getTemplate() {
		return this.template;
	}

	public static class Client {

		/**
		 * Pulsar service URL in the format '(pulsar|pulsar+ssl)://host:port'.
		 */
		private String serviceUrl = "pulsar://localhost:6650";

		/**
		 * Client operation timeout.
		 */
		private Duration operationTimeout = Duration.ofSeconds(30);

		/**
		 * Client lookup timeout.
		 */
		private Duration lookupTimeout = Duration.ofMillis(-1); // FIXME

		/**
		 * Duration to wait for a connection to a broker to be established.
		 */
		private Duration connectionTimeout = Duration.ofSeconds(10);

		/**
		 * Authentication settings.
		 */
		private final Authentication authentication = new Authentication();

		public String getServiceUrl() {
			return this.serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
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

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Authentication getAuthentication() {
			return this.authentication;
		}

	}

	public static class Admin {

		/**
		 * Pulsar web URL for the admin endpoint in the format '(http|https)://host:port'.
		 */
		private String serviceUrl = "http://localhost:8080";

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
		 * Authentication settings.
		 */
		private final Authentication authentication = new Authentication();

		public String getServiceUrl() {
			return this.serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
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

		public Authentication getAuthentication() {
			return this.authentication;
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
				Assert.notNull(messageType, "messageType must not be null");
				Assert.isTrue(topicName != null || schemaInfo != null,
						"At least one of topicName or schemaInfo must not be null");
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
				Assert.notNull(schemaType, "schemaType must not be null");
				Assert.isTrue(schemaType != SchemaType.NONE, "schemaType 'NONE' not supported");
				Assert.isTrue(messageKeyType == null || schemaType == SchemaType.KEY_VALUE,
						"messageKeyType can only be set when schemaType is KEY_VALUE");
			}

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

		public boolean isFailFast() {
			return this.failFast;
		}

		public void setFailFast(boolean failFast) {
			this.failFast = failFast;
		}

		public boolean isPropagateFailures() {
			return this.propagateFailures;
		}

		public void setPropagateFailures(boolean propagateFailures) {
			this.propagateFailures = propagateFailures;
		}

		public boolean isPropagateStopFailures() {
			return this.propagateStopFailures;
		}

		public void setPropagateStopFailures(boolean propagateStopFailures) {
			this.propagateStopFailures = propagateStopFailures;
		}

	}

	public static class Producer {

		/**
		 * Name for the producer. If not assigned, a unique name is generated.
		 */
		private String name;

		/**
		 * Topic the producer will publish to.
		 */
		private String topicName;

		/**
		 * Time before a message has to be acknowledged by the broker.
		 */
		private Duration sendTimeout = Duration.ofSeconds(30);

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
		 * Whether to automatically batch messages.
		 */
		private boolean batchingEnabled = true;

		/**
		 * Whether to split large-size messages into multiple chunks.
		 */
		private boolean chunkingEnabled;

		/**
		 * Message compression type.
		 */
		private CompressionType compressionType;

		/**
		 * Type of access to the topic the producer requires.
		 */
		private ProducerAccessMode accessMode = ProducerAccessMode.Shared;

		private final Cache cache = new Cache();

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTopicName() {
			return this.topicName;
		}

		public void setTopicName(String topicName) {
			this.topicName = topicName;
		}

		public Duration getSendTimeout() {
			return this.sendTimeout;
		}

		public void setSendTimeout(Duration sendTimeout) {
			this.sendTimeout = sendTimeout;
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

		public boolean isBatchingEnabled() {
			return this.batchingEnabled;
		}

		public void setBatchingEnabled(boolean batchingEnabled) {
			this.batchingEnabled = batchingEnabled;
		}

		public boolean isChunkingEnabled() {
			return this.chunkingEnabled;
		}

		public void setChunkingEnabled(boolean chunkingEnabled) {
			this.chunkingEnabled = chunkingEnabled;
		}

		public CompressionType getCompressionType() {
			return this.compressionType;
		}

		public void setCompressionType(CompressionType compressionType) {
			this.compressionType = compressionType;
		}

		public ProducerAccessMode getAccessMode() {
			return this.accessMode;
		}

		public void setAccessMode(ProducerAccessMode accessMode) {
			this.accessMode = accessMode;
		}

		public Cache getCache() {
			return this.cache;
		}

		public static class Cache {

			/**
			 * Time period to expire unused entries in the cache.
			 */
			private Duration expireAfterAccess = Duration.ofMinutes(1);

			/**
			 * Maximum size of cache (entries).
			 */
			private long maximumSize = 1000L;

			/**
			 * Initial size of cache.
			 */
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

	}

	public static class Consumer {

		/**
		 * Consumer name to identify a particular consumer from the topic stats.
		 */
		private String name;

		/**
		 * Topics the consumer subscribes to.
		 */
		private List<String> topics;

		/**
		 * Pattern for topics the consumer subscribes to.
		 */
		private Pattern topicsPattern;

		/**
		 * Priority level for shared subscription consumers.
		 */
		private int priorityLevel = 0;

		/**
		 * Whether to read messages from the compacted topic rather than the full message
		 * backlog.
		 */
		private boolean readCompacted = false;

		/**
		 * Dead letter policy to use.
		 */
		@NestedConfigurationProperty
		private DeadLetterPolicy deadLetterPolicy;

		/**
		 * Consumer subscription properties.
		 */
		private final Subscription subscription = new Subscription();

		/**
		 * Whether to auto retry messages.
		 */
		private boolean retryEnable = false;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Consumer.Subscription getSubscription() {
			return this.subscription;
		}

		public List<String> getTopics() {
			return this.topics;
		}

		public void setTopics(List<String> topics) {
			this.topics = topics;
		}

		public Pattern getTopicsPattern() {
			return this.topicsPattern;
		}

		public void setTopicsPattern(Pattern topicsPattern) {
			this.topicsPattern = topicsPattern;
		}

		public int getPriorityLevel() {
			return this.priorityLevel;
		}

		public void setPriorityLevel(int priorityLevel) {
			this.priorityLevel = priorityLevel;
		}

		public boolean isReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public DeadLetterPolicy getDeadLetterPolicy() {
			return this.deadLetterPolicy;
		}

		public void setDeadLetterPolicy(DeadLetterPolicy deadLetterPolicy) {
			this.deadLetterPolicy = deadLetterPolicy;
		}

		public boolean isRetryEnable() {
			return this.retryEnable;
		}

		public void setRetryEnable(boolean retryEnable) {
			this.retryEnable = retryEnable;
		}

		public static class Subscription {

			/**
			 * Subscription name for the consumer.
			 */
			private String name;

			/**
			 * Position where to initialize a newly created subscription.
			 */
			private SubscriptionInitialPosition initialPosition = SubscriptionInitialPosition.Latest;

			/**
			 * Subscription mode to be used when subscribing to the topic.
			 */
			private SubscriptionMode mode = SubscriptionMode.Durable;

			/**
			 * Determines which type of topics (persistent, non-persistent, or all) the
			 * consumer should be subscribed to when using pattern subscriptions.
			 */
			private RegexSubscriptionMode topicsMode = RegexSubscriptionMode.PersistentOnly;

			/**
			 * Subscription type to be used when subscribing to a topic.
			 */
			private SubscriptionType type = SubscriptionType.Exclusive;

			public String getName() {
				return this.name;
			}

			public void setName(String name) {
				this.name = name;
			}

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

			public RegexSubscriptionMode getTopicsMode() {
				return this.topicsMode;
			}

			public void setTopicsMode(RegexSubscriptionMode topicsMode) {
				this.topicsMode = topicsMode;
			}

			public SubscriptionType getType() {
				return this.type;
			}

			public void setType(SubscriptionType type) {
				this.type = type;
			}

		}

		public static class DeadLetterPolicy {

			/**
			 * Maximum number of times that a message will be redelivered before being
			 * sent to the dead letter queue.
			 */
			private int maxRedeliverCount;

			/**
			 * Name of the retry topic where the failing messages will be sent.
			 */
			private String retryLetterTopic;

			/**
			 * Name of the dead topic where the failing messages will be sent.
			 */
			private String deadLetterTopic;

			/**
			 * Name of the initial subscription of the dead letter topic. When not set,
			 * the initial subscription will not be created. However, when the property is
			 * set then the broker's 'allowAutoSubscriptionCreation' must be enabled or
			 * the DLQ producer will fail.
			 */
			private String initialSubscriptionName;

			public int getMaxRedeliverCount() {
				return this.maxRedeliverCount;
			}

			public void setMaxRedeliverCount(int maxRedeliverCount) {
				this.maxRedeliverCount = maxRedeliverCount;
			}

			public String getRetryLetterTopic() {
				return this.retryLetterTopic;
			}

			public void setRetryLetterTopic(String retryLetterTopic) {
				this.retryLetterTopic = retryLetterTopic;
			}

			public String getDeadLetterTopic() {
				return this.deadLetterTopic;
			}

			public void setDeadLetterTopic(String deadLetterTopic) {
				this.deadLetterTopic = deadLetterTopic;
			}

			public String getInitialSubscriptionName() {
				return this.initialSubscriptionName;
			}

			public void setInitialSubscriptionName(String initialSubscriptionName) {
				this.initialSubscriptionName = initialSubscriptionName;
			}

		}

	}

	public static class Listener {

		/**
		 * SchemaType of the consumed messages.
		 */
		private SchemaType schemaType;

		/**
		 * Whether to record observations for when the Observations API is available and
		 * the client supports it.
		 */
		private boolean observationEnabled = true;

		public SchemaType getSchemaType() {
			return this.schemaType;
		}

		public void setSchemaType(SchemaType schemaType) {
			this.schemaType = schemaType;
		}

		public boolean isObservationEnabled() {
			return this.observationEnabled;
		}

		public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

	}

	public static class Reader {

		/**
		 * Reader name.
		 */
		private String name;

		/**
		 * Topis the reader subscribes to.
		 */
		private List<String> topics;

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
		private boolean readCompacted;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getTopics() {
			return this.topics;
		}

		public void setTopics(List<String> topics) {
			this.topics = topics;
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

		public boolean isReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

	}

	public static class Template {

		/**
		 * Whether to record observations for when the Observations API is available.
		 */
		private boolean observationsEnabled = true;

		public boolean isObservationsEnabled() {
			return this.observationsEnabled;
		}

		public void setObservationsEnabled(boolean observationsEnabled) {
			this.observationsEnabled = observationsEnabled;
		}

	}

	public static class Authentication {

		/**
		 * Fully qualified class name of the authentication plugin.
		 */
		private String pluginClassName;

		/**
		 * Authentication parameter(s) as a map of parameter names to parameter values.
		 */
		private Map<String, String> param = new LinkedHashMap<>();

		public String getPluginClassName() {
			return this.pluginClassName;
		}

		public void setPluginClassName(String pluginClassName) {
			this.pluginClassName = pluginClassName;
		}

		public Map<String, String> getParam() {
			return this.param;
		}

		public void setParam(Map<String, String> param) {
			this.param = param;
		}

	}

}
