/*
 * Copyright 2012-2024 the original author or authors.
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

import org.apache.pulsar.client.api.AutoClusterFailoverBuilder.FailoverPolicy;
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
 * @author Swamy Mavuri
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

	/**
     * Returns the client associated with this PulsarProperties instance.
     *
     * @return the client associated with this PulsarProperties instance
     */
    public Client getClient() {
		return this.client;
	}

	/**
     * Returns the admin object associated with this PulsarProperties instance.
     *
     * @return the admin object
     */
    public Admin getAdmin() {
		return this.admin;
	}

	/**
     * Returns the default values for the PulsarProperties.
     *
     * @return the default values for the PulsarProperties
     */
    public Defaults getDefaults() {
		return this.defaults;
	}

	/**
     * Returns the producer associated with this PulsarProperties object.
     *
     * @return the producer associated with this PulsarProperties object
     */
    public Producer getProducer() {
		return this.producer;
	}

	/**
     * Returns the consumer associated with this PulsarProperties object.
     *
     * @return the consumer associated with this PulsarProperties object
     */
    public Consumer getConsumer() {
		return this.consumer;
	}

	/**
     * Returns the listener associated with this PulsarProperties object.
     *
     * @return the listener associated with this PulsarProperties object
     */
    public Listener getListener() {
		return this.listener;
	}

	/**
     * Returns the reader object associated with this PulsarProperties instance.
     *
     * @return the reader object
     */
    public Reader getReader() {
		return this.reader;
	}

	/**
     * Returns the function associated with this PulsarProperties object.
     *
     * @return the function associated with this PulsarProperties object
     */
    public Function getFunction() {
		return this.function;
	}

	/**
     * Returns the template associated with this PulsarProperties object.
     *
     * @return the template associated with this PulsarProperties object
     */
    public Template getTemplate() {
		return this.template;
	}

	/**
     * Client class.
     */
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
		private Duration lookupTimeout;

		/**
		 * Duration to wait for a connection to a broker to be established.
		 */
		private Duration connectionTimeout = Duration.ofSeconds(10);

		/**
		 * Authentication settings.
		 */
		private final Authentication authentication = new Authentication();

		/**
		 * Failover settings.
		 */
		private final Failover failover = new Failover();

		/**
         * Returns the service URL.
         * 
         * @return the service URL
         */
        public String getServiceUrl() {
			return this.serviceUrl;
		}

		/**
         * Sets the service URL for the client.
         * 
         * @param serviceUrl the URL of the service to be set
         */
        public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}

		/**
         * Returns the operation timeout duration.
         *
         * @return the operation timeout duration
         */
        public Duration getOperationTimeout() {
			return this.operationTimeout;
		}

		/**
         * Sets the operation timeout for the client.
         * 
         * @param operationTimeout the duration of the operation timeout
         */
        public void setOperationTimeout(Duration operationTimeout) {
			this.operationTimeout = operationTimeout;
		}

		/**
         * Returns the lookup timeout duration.
         *
         * @return the lookup timeout duration
         */
        public Duration getLookupTimeout() {
			return this.lookupTimeout;
		}

		/**
         * Sets the lookup timeout for the client.
         * 
         * @param lookupTimeout the duration of the lookup timeout
         */
        public void setLookupTimeout(Duration lookupTimeout) {
			this.lookupTimeout = lookupTimeout;
		}

		/**
         * Returns the connection timeout duration.
         *
         * @return the connection timeout duration
         */
        public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		/**
         * Sets the connection timeout for the client.
         * 
         * @param connectionTimeout the duration of the connection timeout
         */
        public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		/**
         * Returns the authentication object associated with this client.
         *
         * @return the authentication object
         */
        public Authentication getAuthentication() {
			return this.authentication;
		}

		/**
         * Returns the failover object associated with this client.
         *
         * @return the failover object
         */
        public Failover getFailover() {
			return this.failover;
		}

	}

	/**
     * Admin class.
     */
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

		/**
         * Returns the service URL.
         * 
         * @return the service URL
         */
        public String getServiceUrl() {
			return this.serviceUrl;
		}

		/**
         * Sets the service URL for the Admin class.
         * 
         * @param serviceUrl the service URL to be set
         */
        public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}

		/**
         * Returns the connection timeout duration.
         *
         * @return the connection timeout duration
         */
        public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		/**
         * Sets the connection timeout for the Admin class.
         * 
         * @param connectionTimeout the duration of the connection timeout
         */
        public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		/**
         * Returns the read timeout duration.
         *
         * @return the read timeout duration
         */
        public Duration getReadTimeout() {
			return this.readTimeout;
		}

		/**
         * Sets the read timeout for the Admin class.
         * 
         * @param readTimeout the duration of the read timeout
         */
        public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

		/**
         * Returns the request timeout duration.
         *
         * @return the request timeout duration
         */
        public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		/**
         * Sets the request timeout for the Admin class.
         * 
         * @param requestTimeout the duration of the request timeout
         */
        public void setRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

		/**
         * Returns the authentication object associated with this Admin instance.
         *
         * @return the authentication object
         */
        public Authentication getAuthentication() {
			return this.authentication;
		}

	}

	/**
     * Defaults class.
     */
    public static class Defaults {

		/**
		 * List of mappings from message type to topic name and schema info to use as a
		 * defaults when a topic name and/or schema is not explicitly specified when
		 * producing or consuming messages of the mapped type.
		 */
		private List<TypeMapping> typeMappings = new ArrayList<>();

		/**
         * Returns the list of TypeMappings.
         *
         * @return the list of TypeMappings
         */
        public List<TypeMapping> getTypeMappings() {
			return this.typeMappings;
		}

		/**
         * Sets the type mappings for this Defaults object.
         * 
         * @param typeMappings the list of TypeMapping objects to be set
         */
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
		public record /**
         * Creates a new TypeMapping object with the specified messageType, topicName, and schemaInfo.
         * 
         * @param messageType the class representing the type of the message
         * @param topicName the name of the topic associated with the message type
         * @param schemaInfo the schema information associated with the message type
         * @throws IllegalArgumentException if messageType is null or if both topicName and schemaInfo are null
         */
        TypeMapping(Class<?> messageType, String topicName, SchemaInfo schemaInfo) {

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
		public record /**
         * Constructs a new SchemaInfo object with the given schema type and message key type.
         * 
         * @param schemaType the type of schema to be used
         * @param messageKeyType the type of message key, can only be set when schemaType is KEY_VALUE
         * @throws IllegalArgumentException if schemaType is null or 'NONE', or if messageKeyType is set when schemaType is not KEY_VALUE
         */
        SchemaInfo(SchemaType schemaType, Class<?> messageKeyType) {

			public SchemaInfo {
				Assert.notNull(schemaType, "schemaType must not be null");
				Assert.isTrue(schemaType != SchemaType.NONE, "schemaType 'NONE' not supported");
				Assert.isTrue(messageKeyType == null || schemaType == SchemaType.KEY_VALUE,
						"messageKeyType can only be set when schemaType is KEY_VALUE");
			}

		}

	}

	/**
     * Function class.
     */
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

		/**
         * Returns whether the function is set to fail fast.
         * 
         * @return {@code true} if the function is set to fail fast, {@code false} otherwise.
         */
        public boolean isFailFast() {
			return this.failFast;
		}

		/**
         * Sets the fail fast flag.
         * 
         * @param failFast the value to set the fail fast flag to
         */
        public void setFailFast(boolean failFast) {
			this.failFast = failFast;
		}

		/**
         * Returns a boolean value indicating whether failures should be propagated.
         * 
         * @return true if failures should be propagated, false otherwise
         */
        public boolean isPropagateFailures() {
			return this.propagateFailures;
		}

		/**
         * Sets the flag indicating whether failures should be propagated.
         * 
         * @param propagateFailures true if failures should be propagated, false otherwise
         */
        public void setPropagateFailures(boolean propagateFailures) {
			this.propagateFailures = propagateFailures;
		}

		/**
         * Returns the value indicating whether stop failures should be propagated.
         * 
         * @return true if stop failures should be propagated, false otherwise
         */
        public boolean isPropagateStopFailures() {
			return this.propagateStopFailures;
		}

		/**
         * Sets the flag indicating whether to propagate stop failures.
         * 
         * @param propagateStopFailures true to propagate stop failures, false otherwise
         */
        public void setPropagateStopFailures(boolean propagateStopFailures) {
			this.propagateStopFailures = propagateStopFailures;
		}

	}

	/**
     * Producer class.
     */
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

		/**
         * Returns the name of the Producer.
         *
         * @return the name of the Producer
         */
        public String getName() {
			return this.name;
		}

		/**
         * Sets the name of the producer.
         * 
         * @param name the name to set
         */
        public void setName(String name) {
			this.name = name;
		}

		/**
         * Returns the name of the topic.
         *
         * @return the name of the topic
         */
        public String getTopicName() {
			return this.topicName;
		}

		/**
         * Sets the name of the topic for the producer.
         * 
         * @param topicName the name of the topic to be set
         */
        public void setTopicName(String topicName) {
			this.topicName = topicName;
		}

		/**
         * Returns the send timeout duration.
         *
         * @return the send timeout duration
         */
        public Duration getSendTimeout() {
			return this.sendTimeout;
		}

		/**
         * Sets the send timeout for the producer.
         * 
         * @param sendTimeout the send timeout duration to be set
         */
        public void setSendTimeout(Duration sendTimeout) {
			this.sendTimeout = sendTimeout;
		}

		/**
         * Returns the message routing mode of the Producer.
         *
         * @return the message routing mode of the Producer
         */
        public MessageRoutingMode getMessageRoutingMode() {
			return this.messageRoutingMode;
		}

		/**
         * Sets the message routing mode for the producer.
         * 
         * @param messageRoutingMode the message routing mode to be set
         */
        public void setMessageRoutingMode(MessageRoutingMode messageRoutingMode) {
			this.messageRoutingMode = messageRoutingMode;
		}

		/**
         * Returns the hashing scheme used by the producer.
         * 
         * @return the hashing scheme used by the producer
         */
        public HashingScheme getHashingScheme() {
			return this.hashingScheme;
		}

		/**
         * Sets the hashing scheme for the producer.
         * 
         * @param hashingScheme the hashing scheme to be set
         */
        public void setHashingScheme(HashingScheme hashingScheme) {
			this.hashingScheme = hashingScheme;
		}

		/**
         * Returns a boolean value indicating whether batching is enabled.
         * 
         * @return true if batching is enabled, false otherwise
         */
        public boolean isBatchingEnabled() {
			return this.batchingEnabled;
		}

		/**
         * Sets the flag indicating whether batching is enabled for the producer.
         * 
         * @param batchingEnabled true if batching is enabled, false otherwise
         */
        public void setBatchingEnabled(boolean batchingEnabled) {
			this.batchingEnabled = batchingEnabled;
		}

		/**
         * Returns a boolean value indicating whether chunking is enabled.
         * 
         * @return {@code true} if chunking is enabled, {@code false} otherwise
         */
        public boolean isChunkingEnabled() {
			return this.chunkingEnabled;
		}

		/**
         * Sets the flag indicating whether chunking is enabled or not.
         * 
         * @param chunkingEnabled true if chunking is enabled, false otherwise
         */
        public void setChunkingEnabled(boolean chunkingEnabled) {
			this.chunkingEnabled = chunkingEnabled;
		}

		/**
         * Returns the compression type used by the producer.
         *
         * @return the compression type used by the producer
         */
        public CompressionType getCompressionType() {
			return this.compressionType;
		}

		/**
         * Sets the compression type for the producer.
         * 
         * @param compressionType the compression type to be set
         */
        public void setCompressionType(CompressionType compressionType) {
			this.compressionType = compressionType;
		}

		/**
         * Returns the access mode of the producer.
         *
         * @return the access mode of the producer
         */
        public ProducerAccessMode getAccessMode() {
			return this.accessMode;
		}

		/**
         * Sets the access mode for the producer.
         * 
         * @param accessMode the access mode to be set
         */
        public void setAccessMode(ProducerAccessMode accessMode) {
			this.accessMode = accessMode;
		}

		/**
         * Returns the cache object.
         * 
         * @return the cache object
         */
        public Cache getCache() {
			return this.cache;
		}

		/**
         * Cache class.
         */
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

			/**
             * Returns the duration after which an entry in the cache will expire after its last access.
             *
             * @return the duration after which an entry will expire after its last access
             */
            public Duration getExpireAfterAccess() {
				return this.expireAfterAccess;
			}

			/**
             * Sets the duration after which the cache entries should expire after last access.
             * 
             * @param expireAfterAccess the duration after which cache entries should expire after last access
             */
            public void setExpireAfterAccess(Duration expireAfterAccess) {
				this.expireAfterAccess = expireAfterAccess;
			}

			/**
             * Returns the maximum size of the cache.
             *
             * @return the maximum size of the cache
             */
            public long getMaximumSize() {
				return this.maximumSize;
			}

			/**
             * Sets the maximum size of the cache.
             * 
             * @param maximumSize the maximum size of the cache to be set
             */
            public void setMaximumSize(long maximumSize) {
				this.maximumSize = maximumSize;
			}

			/**
             * Returns the initial capacity of the cache.
             *
             * @return the initial capacity of the cache
             */
            public int getInitialCapacity() {
				return this.initialCapacity;
			}

			/**
             * Sets the initial capacity of the cache.
             * 
             * @param initialCapacity the initial capacity to set
             */
            public void setInitialCapacity(int initialCapacity) {
				this.initialCapacity = initialCapacity;
			}

		}

	}

	/**
     * Consumer class.
     */
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

		/**
         * Returns the name of the consumer.
         *
         * @return the name of the consumer
         */
        public String getName() {
			return this.name;
		}

		/**
         * Sets the name of the consumer.
         * 
         * @param name the name to be set
         */
        public void setName(String name) {
			this.name = name;
		}

		/**
         * Returns the subscription associated with this Consumer.
         *
         * @return the subscription associated with this Consumer
         */
        public Consumer.Subscription getSubscription() {
			return this.subscription;
		}

		/**
         * Returns the list of topics.
         * 
         * @return the list of topics
         */
        public List<String> getTopics() {
			return this.topics;
		}

		/**
         * Sets the topics for the consumer.
         * 
         * @param topics the list of topics to be set
         */
        public void setTopics(List<String> topics) {
			this.topics = topics;
		}

		/**
         * Returns the pattern used to match topics in the Consumer class.
         *
         * @return the pattern used to match topics
         */
        public Pattern getTopicsPattern() {
			return this.topicsPattern;
		}

		/**
         * Sets the pattern for filtering topics in the Consumer.
         * 
         * @param topicsPattern the pattern to be set for filtering topics
         */
        public void setTopicsPattern(Pattern topicsPattern) {
			this.topicsPattern = topicsPattern;
		}

		/**
         * Returns the priority level of the consumer.
         *
         * @return the priority level of the consumer
         */
        public int getPriorityLevel() {
			return this.priorityLevel;
		}

		/**
         * Sets the priority level for the consumer.
         * 
         * @param priorityLevel the priority level to be set
         */
        public void setPriorityLevel(int priorityLevel) {
			this.priorityLevel = priorityLevel;
		}

		/**
         * Returns a boolean value indicating whether the consumer is set to read compacted topics.
         *
         * @return true if the consumer is set to read compacted topics, false otherwise
         */
        public boolean isReadCompacted() {
			return this.readCompacted;
		}

		/**
         * Sets whether the consumer should read compacted messages.
         * 
         * @param readCompacted true if the consumer should read compacted messages, false otherwise
         */
        public void setReadCompacted(boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		/**
         * Returns the dead letter policy associated with this consumer.
         *
         * @return the dead letter policy
         */
        public DeadLetterPolicy getDeadLetterPolicy() {
			return this.deadLetterPolicy;
		}

		/**
         * Sets the dead letter policy for the consumer.
         * 
         * @param deadLetterPolicy the dead letter policy to be set
         */
        public void setDeadLetterPolicy(DeadLetterPolicy deadLetterPolicy) {
			this.deadLetterPolicy = deadLetterPolicy;
		}

		/**
         * Returns a boolean value indicating whether retry is enabled.
         * 
         * @return true if retry is enabled, false otherwise
         */
        public boolean isRetryEnable() {
			return this.retryEnable;
		}

		/**
         * Sets the retry enable flag.
         * 
         * @param retryEnable the retry enable flag to set
         */
        public void setRetryEnable(boolean retryEnable) {
			this.retryEnable = retryEnable;
		}

		/**
         * Subscription class.
         */
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

			/**
             * Returns the name of the subscription.
             *
             * @return the name of the subscription
             */
            public String getName() {
				return this.name;
			}

			/**
             * Sets the name of the subscription.
             * 
             * @param name the name to set for the subscription
             */
            public void setName(String name) {
				this.name = name;
			}

			/**
             * Returns the initial position of the subscription.
             *
             * @return the initial position of the subscription
             */
            public SubscriptionInitialPosition getInitialPosition() {
				return this.initialPosition;
			}

			/**
             * Sets the initial position for the subscription.
             * 
             * @param initialPosition the initial position to be set
             */
            public void setInitialPosition(SubscriptionInitialPosition initialPosition) {
				this.initialPosition = initialPosition;
			}

			/**
             * Returns the subscription mode.
             * 
             * @return the subscription mode
             */
            public SubscriptionMode getMode() {
				return this.mode;
			}

			/**
             * Sets the mode of the subscription.
             * 
             * @param mode the mode to be set for the subscription
             */
            public void setMode(SubscriptionMode mode) {
				this.mode = mode;
			}

			/**
             * Returns the subscription mode for topics.
             * 
             * @return the subscription mode for topics
             */
            public RegexSubscriptionMode getTopicsMode() {
				return this.topicsMode;
			}

			/**
             * Sets the mode for subscribing to topics.
             * 
             * @param topicsMode the mode for subscribing to topics
             */
            public void setTopicsMode(RegexSubscriptionMode topicsMode) {
				this.topicsMode = topicsMode;
			}

			/**
             * Returns the type of the subscription.
             * 
             * @return the type of the subscription
             */
            public SubscriptionType getType() {
				return this.type;
			}

			/**
             * Sets the type of the subscription.
             * 
             * @param type the type of the subscription
             */
            public void setType(SubscriptionType type) {
				this.type = type;
			}

		}

		/**
         * DeadLetterPolicy class.
         */
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

			/**
             * Returns the maximum redelivery count for messages in the DeadLetterPolicy.
             *
             * @return the maximum redelivery count
             */
            public int getMaxRedeliverCount() {
				return this.maxRedeliverCount;
			}

			/**
             * Sets the maximum redelivery count for the DeadLetterPolicy.
             * 
             * @param maxRedeliverCount the maximum redelivery count to be set
             */
            public void setMaxRedeliverCount(int maxRedeliverCount) {
				this.maxRedeliverCount = maxRedeliverCount;
			}

			/**
             * Returns the topic of the retry letter.
             *
             * @return the topic of the retry letter
             */
            public String getRetryLetterTopic() {
				return this.retryLetterTopic;
			}

			/**
             * Sets the topic for retry letters.
             * 
             * @param retryLetterTopic the topic for retry letters
             */
            public void setRetryLetterTopic(String retryLetterTopic) {
				this.retryLetterTopic = retryLetterTopic;
			}

			/**
             * Returns the dead letter topic associated with the DeadLetterPolicy.
             *
             * @return the dead letter topic
             */
            public String getDeadLetterTopic() {
				return this.deadLetterTopic;
			}

			/**
             * Sets the dead letter topic for the DeadLetterPolicy.
             * 
             * @param deadLetterTopic the topic to be set as the dead letter topic
             */
            public void setDeadLetterTopic(String deadLetterTopic) {
				this.deadLetterTopic = deadLetterTopic;
			}

			/**
             * Returns the initial subscription name.
             *
             * @return the initial subscription name
             */
            public String getInitialSubscriptionName() {
				return this.initialSubscriptionName;
			}

			/**
             * Sets the initial subscription name for the DeadLetterPolicy.
             * 
             * @param initialSubscriptionName the initial subscription name to be set
             */
            public void setInitialSubscriptionName(String initialSubscriptionName) {
				this.initialSubscriptionName = initialSubscriptionName;
			}

		}

	}

	/**
     * Listener class.
     */
    public static class Listener {

		/**
		 * SchemaType of the consumed messages.
		 */
		private SchemaType schemaType;

		/**
		 * Whether to record observations for when the Observations API is available and
		 * the client supports it.
		 */
		private boolean observationEnabled;

		/**
         * Returns the schema type of the listener.
         * 
         * @return the schema type of the listener
         */
        public SchemaType getSchemaType() {
			return this.schemaType;
		}

		/**
         * Sets the schema type for the listener.
         * 
         * @param schemaType the schema type to be set
         */
        public void setSchemaType(SchemaType schemaType) {
			this.schemaType = schemaType;
		}

		/**
         * Returns a boolean value indicating whether observation is enabled.
         * 
         * @return true if observation is enabled, false otherwise
         */
        public boolean isObservationEnabled() {
			return this.observationEnabled;
		}

		/**
         * Sets the flag indicating whether observation is enabled or not.
         * 
         * @param observationEnabled true if observation is enabled, false otherwise
         */
        public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

	}

	/**
     * Reader class.
     */
    public static class Reader {

		/**
		 * Reader name.
		 */
		private String name;

		/**
		 * Topics the reader subscribes to.
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

		/**
         * Returns the name of the Reader.
         *
         * @return the name of the Reader
         */
        public String getName() {
			return this.name;
		}

		/**
         * Sets the name of the reader.
         * 
         * @param name the name of the reader
         */
        public void setName(String name) {
			this.name = name;
		}

		/**
         * Returns the list of topics.
         * 
         * @return the list of topics
         */
        public List<String> getTopics() {
			return this.topics;
		}

		/**
         * Sets the topics for the reader.
         * 
         * @param topics the list of topics to be set
         */
        public void setTopics(List<String> topics) {
			this.topics = topics;
		}

		/**
         * Returns the subscription name.
         *
         * @return the subscription name
         */
        public String getSubscriptionName() {
			return this.subscriptionName;
		}

		/**
         * Sets the subscription name for the Reader.
         * 
         * @param subscriptionName the name of the subscription to be set
         */
        public void setSubscriptionName(String subscriptionName) {
			this.subscriptionName = subscriptionName;
		}

		/**
         * Returns the subscription role prefix.
         * 
         * @return the subscription role prefix
         */
        public String getSubscriptionRolePrefix() {
			return this.subscriptionRolePrefix;
		}

		/**
         * Sets the subscription role prefix.
         * 
         * @param subscriptionRolePrefix the prefix to be set for the subscription role
         */
        public void setSubscriptionRolePrefix(String subscriptionRolePrefix) {
			this.subscriptionRolePrefix = subscriptionRolePrefix;
		}

		/**
         * Returns a boolean value indicating whether the reader is set to read compacted data.
         * 
         * @return true if the reader is set to read compacted data, false otherwise
         */
        public boolean isReadCompacted() {
			return this.readCompacted;
		}

		/**
         * Sets the flag indicating whether to read compacted data.
         * 
         * @param readCompacted true to read compacted data, false otherwise
         */
        public void setReadCompacted(boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

	}

	/**
     * Template class.
     */
    public static class Template {

		/**
		 * Whether to record observations for when the Observations API is available.
		 */
		private boolean observationsEnabled;

		/**
         * Returns a boolean value indicating whether observations are enabled.
         * 
         * @return true if observations are enabled, false otherwise
         */
        public boolean isObservationsEnabled() {
			return this.observationsEnabled;
		}

		/**
         * Sets the flag indicating whether observations are enabled.
         * 
         * @param observationsEnabled the flag indicating whether observations are enabled
         */
        public void setObservationsEnabled(boolean observationsEnabled) {
			this.observationsEnabled = observationsEnabled;
		}

	}

	/**
     * Authentication class.
     */
    public static class Authentication {

		/**
		 * Fully qualified class name of the authentication plugin.
		 */
		private String pluginClassName;

		/**
		 * Authentication parameter(s) as a map of parameter names to parameter values.
		 */
		private Map<String, String> param = new LinkedHashMap<>();

		/**
         * Returns the fully qualified class name of the plugin.
         *
         * @return the fully qualified class name of the plugin
         */
        public String getPluginClassName() {
			return this.pluginClassName;
		}

		/**
         * Sets the plugin class name for authentication.
         * 
         * @param pluginClassName the fully qualified class name of the authentication plugin
         */
        public void setPluginClassName(String pluginClassName) {
			this.pluginClassName = pluginClassName;
		}

		/**
         * Returns the parameter map.
         *
         * @return the parameter map
         */
        public Map<String, String> getParam() {
			return this.param;
		}

		/**
         * Sets the parameters for authentication.
         * 
         * @param param a map containing the parameters for authentication
         */
        public void setParam(Map<String, String> param) {
			this.param = param;
		}

	}

	/**
     * Failover class.
     */
    public static class Failover {

		/**
		 * Cluster Failover Policy.
		 */
		private FailoverPolicy failoverPolicy = FailoverPolicy.ORDER;

		/**
		 * Delay before the Pulsar client switches from the primary cluster to the backup
		 * cluster.
		 */
		private Duration failOverDelay;

		/**
		 * Delay before the Pulsar client switches from the backup cluster to the primary
		 * cluster.
		 */
		private Duration switchBackDelay;

		/**
		 * Frequency of performing a probe task.
		 */
		private Duration checkInterval;

		/**
		 * List of backupClusters The backup cluster is chosen in the sequence of the
		 * given list. If all backup clusters are available, the Pulsar client chooses the
		 * first backup cluster.
		 */
		private List<BackupCluster> backupClusters = new ArrayList<>();

		/**
         * Returns the failover policy of the Failover object.
         *
         * @return the failover policy of the Failover object
         */
        public FailoverPolicy getFailoverPolicy() {
			return this.failoverPolicy;
		}

		/**
         * Sets the failover policy for the Failover class.
         * 
         * @param failoverPolicy the failover policy to be set
         */
        public void setFailoverPolicy(FailoverPolicy failoverPolicy) {
			this.failoverPolicy = failoverPolicy;
		}

		/**
         * Returns the failover delay.
         *
         * @return the failover delay
         */
        public Duration getFailOverDelay() {
			return this.failOverDelay;
		}

		/**
         * Sets the delay for failover.
         * 
         * @param failOverDelay the duration of the delay for failover
         */
        public void setFailOverDelay(Duration failOverDelay) {
			this.failOverDelay = failOverDelay;
		}

		/**
         * Returns the switch back delay.
         *
         * @return the switch back delay
         */
        public Duration getSwitchBackDelay() {
			return this.switchBackDelay;
		}

		/**
         * Sets the delay for switching back to the primary server after a failover.
         * 
         * @param switchBackDelay the duration of the delay
         */
        public void setSwitchBackDelay(Duration switchBackDelay) {
			this.switchBackDelay = switchBackDelay;
		}

		/**
         * Returns the check interval for failover.
         *
         * @return the check interval for failover
         */
        public Duration getCheckInterval() {
			return this.checkInterval;
		}

		/**
         * Sets the check interval for failover.
         * 
         * @param checkInterval the duration between each failover check
         */
        public void setCheckInterval(Duration checkInterval) {
			this.checkInterval = checkInterval;
		}

		/**
         * Returns the list of backup clusters.
         * 
         * @return the list of backup clusters
         */
        public List<BackupCluster> getBackupClusters() {
			return this.backupClusters;
		}

		/**
         * Sets the list of backup clusters for failover.
         * 
         * @param backupClusters the list of backup clusters to be set
         */
        public void setBackupClusters(List<BackupCluster> backupClusters) {
			this.backupClusters = backupClusters;
		}

		/**
         * BackupCluster class.
         */
        public static class BackupCluster {

			/**
			 * Pulsar service URL in the format '(pulsar|pulsar+ssl)://host:port'.
			 */
			private String serviceUrl = "pulsar://localhost:6650";

			/**
			 * Authentication settings.
			 */
			private final Authentication authentication = new Authentication();

			/**
             * Returns the service URL of the BackupCluster.
             *
             * @return the service URL of the BackupCluster
             */
            public String getServiceUrl() {
				return this.serviceUrl;
			}

			/**
             * Sets the service URL for the BackupCluster.
             * 
             * @param serviceUrl the service URL to be set
             */
            public void setServiceUrl(String serviceUrl) {
				this.serviceUrl = serviceUrl;
			}

			/**
             * Returns the authentication object associated with this BackupCluster.
             *
             * @return the authentication object
             */
            public Authentication getAuthentication() {
				return this.authentication;
			}

		}

	}

}
