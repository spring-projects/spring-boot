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

package org.springframework.boot.autoconfigure.kafka;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.io.Resource;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Spring for Apache Kafka.
 * <p>
 * Users should refer to Kafka documentation for complete descriptions of these
 * properties.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @author Nakul Mishra
 * @author Tomaz Fernandes
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

	/**
	 * Comma-delimited list of host:port pairs to use for establishing the initial
	 * connections to the Kafka cluster. Applies to all components unless overridden.
	 */
	private List<String> bootstrapServers = new ArrayList<>(Collections.singletonList("localhost:9092"));

	/**
	 * ID to pass to the server when making requests. Used for server-side logging.
	 */
	private String clientId;

	/**
	 * Additional properties, common to producers and consumers, used to configure the
	 * client.
	 */
	private final Map<String, String> properties = new HashMap<>();

	private final Consumer consumer = new Consumer();

	private final Producer producer = new Producer();

	private final Admin admin = new Admin();

	private final Streams streams = new Streams();

	private final Listener listener = new Listener();

	private final Ssl ssl = new Ssl();

	private final Jaas jaas = new Jaas();

	private final Template template = new Template();

	private final Security security = new Security();

	private final Retry retry = new Retry();

	/**
     * Returns the list of bootstrap servers.
     *
     * @return the list of bootstrap servers
     */
    public List<String> getBootstrapServers() {
		return this.bootstrapServers;
	}

	/**
     * Sets the list of bootstrap servers for connecting to the Kafka cluster.
     * 
     * @param bootstrapServers the list of bootstrap servers to set
     */
    public void setBootstrapServers(List<String> bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	/**
     * Returns the client ID associated with the Kafka properties.
     *
     * @return the client ID
     */
    public String getClientId() {
		return this.clientId;
	}

	/**
     * Sets the client ID for the Kafka properties.
     * 
     * @param clientId the client ID to be set
     */
    public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
     * Returns the properties of the KafkaProperties class.
     *
     * @return a Map containing the properties of the KafkaProperties class
     */
    public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
     * Returns the consumer associated with this KafkaProperties instance.
     *
     * @return the consumer associated with this KafkaProperties instance
     */
    public Consumer getConsumer() {
		return this.consumer;
	}

	/**
     * Returns the producer associated with this KafkaProperties instance.
     *
     * @return the producer associated with this KafkaProperties instance
     */
    public Producer getProducer() {
		return this.producer;
	}

	/**
     * Returns the listener associated with this KafkaProperties instance.
     *
     * @return the listener associated with this KafkaProperties instance
     */
    public Listener getListener() {
		return this.listener;
	}

	/**
     * Returns the admin object associated with this KafkaProperties instance.
     *
     * @return the admin object
     */
    public Admin getAdmin() {
		return this.admin;
	}

	/**
     * Returns the Streams object associated with this KafkaProperties instance.
     *
     * @return the Streams object
     */
    public Streams getStreams() {
		return this.streams;
	}

	/**
     * Returns the SSL configuration for the Kafka properties.
     *
     * @return the SSL configuration for the Kafka properties
     */
    public Ssl getSsl() {
		return this.ssl;
	}

	/**
     * Returns the Jaas object associated with this KafkaProperties instance.
     *
     * @return the Jaas object
     */
    public Jaas getJaas() {
		return this.jaas;
	}

	/**
     * Returns the template object associated with this KafkaProperties instance.
     *
     * @return the template object
     */
    public Template getTemplate() {
		return this.template;
	}

	/**
     * Returns the security configuration for the Kafka properties.
     *
     * @return the security configuration for the Kafka properties
     */
    public Security getSecurity() {
		return this.security;
	}

	/**
     * Returns the retry configuration for the Kafka properties.
     *
     * @return the retry configuration
     */
    public Retry getRetry() {
		return this.retry;
	}

	/**
     * Builds a map of common properties for Kafka clients.
     * 
     * @param sslBundles the SSL bundles to be used for SSL configuration
     * @return a map of common properties for Kafka clients
     */
    private Map<String, Object> buildCommonProperties(SslBundles sslBundles) {
		Map<String, Object> properties = new HashMap<>();
		if (this.bootstrapServers != null) {
			properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		}
		if (this.clientId != null) {
			properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, this.clientId);
		}
		properties.putAll(this.ssl.buildProperties(sslBundles));
		properties.putAll(this.security.buildProperties());
		if (!CollectionUtils.isEmpty(this.properties)) {
			properties.putAll(this.properties);
		}
		return properties;
	}

	/**
	 * Create an initial map of consumer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default {@code kafkaConsumerFactory} bean.
	 * @return the consumer properties initialized with the customizations defined on this
	 * instance
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of
	 * {@link #buildConsumerProperties(SslBundles)}}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Map<String, Object> buildConsumerProperties() {
		return buildConsumerProperties(null);
	}

	/**
	 * Create an initial map of consumer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default {@code kafkaConsumerFactory} bean.
	 * @param sslBundles bundles providing SSL trust material
	 * @return the consumer properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildConsumerProperties(SslBundles sslBundles) {
		Map<String, Object> properties = buildCommonProperties(sslBundles);
		properties.putAll(this.consumer.buildProperties(sslBundles));
		return properties;
	}

	/**
	 * Create an initial map of producer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default {@code kafkaProducerFactory} bean.
	 * @return the producer properties initialized with the customizations defined on this
	 * instance
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of
	 * {@link #buildProducerProperties(SslBundles)}}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Map<String, Object> buildProducerProperties() {
		return buildProducerProperties(null);
	}

	/**
	 * Create an initial map of producer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default {@code kafkaProducerFactory} bean.
	 * @param sslBundles bundles providing SSL trust material
	 * @return the producer properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildProducerProperties(SslBundles sslBundles) {
		Map<String, Object> properties = buildCommonProperties(sslBundles);
		properties.putAll(this.producer.buildProperties(sslBundles));
		return properties;
	}

	/**
	 * Create an initial map of admin properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default {@code kafkaAdmin} bean.
	 * @return the admin properties initialized with the customizations defined on this
	 * instance
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of
	 * {@link #buildAdminProperties(SslBundles)}}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Map<String, Object> buildAdminProperties() {
		return buildAdminProperties(null);
	}

	/**
	 * Create an initial map of admin properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default {@code kafkaAdmin} bean.
	 * @param sslBundles bundles providing SSL trust material
	 * @return the admin properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildAdminProperties(SslBundles sslBundles) {
		Map<String, Object> properties = buildCommonProperties(sslBundles);
		properties.putAll(this.admin.buildProperties(sslBundles));
		return properties;
	}

	/**
	 * Create an initial map of streams properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary.
	 * @return the streams properties initialized with the customizations defined on this
	 * instance
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of
	 * {@link #buildStreamsProperties(SslBundles)}}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Map<String, Object> buildStreamsProperties() {
		return buildStreamsProperties(null);
	}

	/**
	 * Create an initial map of streams properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary.
	 * @param sslBundles bundles providing SSL trust material
	 * @return the streams properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildStreamsProperties(SslBundles sslBundles) {
		Map<String, Object> properties = buildCommonProperties(sslBundles);
		properties.putAll(this.streams.buildProperties(sslBundles));
		return properties;
	}

	/**
     * Consumer class.
     */
    public static class Consumer {

		private final Ssl ssl = new Ssl();

		private final Security security = new Security();

		/**
		 * Frequency with which the consumer offsets are auto-committed to Kafka if
		 * 'enable.auto.commit' is set to true.
		 */
		private Duration autoCommitInterval;

		/**
		 * What to do when there is no initial offset in Kafka or if the current offset no
		 * longer exists on the server.
		 */
		private String autoOffsetReset;

		/**
		 * Comma-delimited list of host:port pairs to use for establishing the initial
		 * connections to the Kafka cluster. Overrides the global property, for consumers.
		 */
		private List<String> bootstrapServers;

		/**
		 * ID to pass to the server when making requests. Used for server-side logging.
		 */
		private String clientId;

		/**
		 * Whether the consumer's offset is periodically committed in the background.
		 */
		private Boolean enableAutoCommit;

		/**
		 * Maximum amount of time the server blocks before answering the fetch request if
		 * there isn't sufficient data to immediately satisfy the requirement given by
		 * "fetch-min-size".
		 */
		private Duration fetchMaxWait;

		/**
		 * Minimum amount of data the server should return for a fetch request.
		 */
		private DataSize fetchMinSize;

		/**
		 * Unique string that identifies the consumer group to which this consumer
		 * belongs.
		 */
		private String groupId;

		/**
		 * Expected time between heartbeats to the consumer coordinator.
		 */
		private Duration heartbeatInterval;

		/**
		 * Isolation level for reading messages that have been written transactionally.
		 */
		private IsolationLevel isolationLevel = IsolationLevel.READ_UNCOMMITTED;

		/**
		 * Deserializer class for keys.
		 */
		private Class<?> keyDeserializer = StringDeserializer.class;

		/**
		 * Deserializer class for values.
		 */
		private Class<?> valueDeserializer = StringDeserializer.class;

		/**
		 * Maximum number of records returned in a single call to poll().
		 */
		private Integer maxPollRecords;

		/**
		 * Additional consumer-specific properties used to configure the client.
		 */
		private final Map<String, String> properties = new HashMap<>();

		/**
         * Returns the SSL object associated with this Consumer.
         *
         * @return the SSL object
         */
        public Ssl getSsl() {
			return this.ssl;
		}

		/**
         * Returns the security object associated with this Consumer.
         *
         * @return the security object
         */
        public Security getSecurity() {
			return this.security;
		}

		/**
         * Returns the auto-commit interval for the Consumer.
         *
         * @return the auto-commit interval as a Duration object
         */
        public Duration getAutoCommitInterval() {
			return this.autoCommitInterval;
		}

		/**
         * Sets the auto commit interval for the consumer.
         * 
         * @param autoCommitInterval the auto commit interval to be set
         */
        public void setAutoCommitInterval(Duration autoCommitInterval) {
			this.autoCommitInterval = autoCommitInterval;
		}

		/**
         * Returns the value of the auto offset reset property.
         * 
         * @return the value of the auto offset reset property
         */
        public String getAutoOffsetReset() {
			return this.autoOffsetReset;
		}

		/**
         * Sets the auto offset reset value for the consumer.
         * 
         * @param autoOffsetReset the auto offset reset value to be set
         */
        public void setAutoOffsetReset(String autoOffsetReset) {
			this.autoOffsetReset = autoOffsetReset;
		}

		/**
         * Returns the list of bootstrap servers.
         *
         * @return the list of bootstrap servers
         */
        public List<String> getBootstrapServers() {
			return this.bootstrapServers;
		}

		/**
         * Sets the list of bootstrap servers for the consumer.
         * 
         * @param bootstrapServers the list of bootstrap servers to be set
         */
        public void setBootstrapServers(List<String> bootstrapServers) {
			this.bootstrapServers = bootstrapServers;
		}

		/**
         * Returns the client ID associated with this Consumer.
         *
         * @return the client ID
         */
        public String getClientId() {
			return this.clientId;
		}

		/**
         * Sets the client ID for the consumer.
         * 
         * @param clientId the client ID to be set
         */
        public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		/**
         * Returns the value of the enableAutoCommit property.
         *
         * @return the value of the enableAutoCommit property
         */
        public Boolean getEnableAutoCommit() {
			return this.enableAutoCommit;
		}

		/**
         * Sets the enableAutoCommit flag for the Consumer.
         * 
         * @param enableAutoCommit the value to set for the enableAutoCommit flag
         */
        public void setEnableAutoCommit(Boolean enableAutoCommit) {
			this.enableAutoCommit = enableAutoCommit;
		}

		/**
         * Returns the maximum wait time for fetching data.
         *
         * @return the maximum wait time for fetching data
         */
        public Duration getFetchMaxWait() {
			return this.fetchMaxWait;
		}

		/**
         * Sets the maximum wait time for fetching data.
         * 
         * @param fetchMaxWait the maximum wait time for fetching data
         */
        public void setFetchMaxWait(Duration fetchMaxWait) {
			this.fetchMaxWait = fetchMaxWait;
		}

		/**
         * Returns the minimum size of data to be fetched.
         * 
         * @return the minimum size of data to be fetched
         */
        public DataSize getFetchMinSize() {
			return this.fetchMinSize;
		}

		/**
         * Sets the minimum size of data to be fetched.
         * 
         * @param fetchMinSize the minimum size of data to be fetched
         */
        public void setFetchMinSize(DataSize fetchMinSize) {
			this.fetchMinSize = fetchMinSize;
		}

		/**
         * Returns the group ID associated with this Consumer.
         *
         * @return the group ID
         */
        public String getGroupId() {
			return this.groupId;
		}

		/**
         * Sets the group ID for the consumer.
         * 
         * @param groupId the group ID to be set
         */
        public void setGroupId(String groupId) {
			this.groupId = groupId;
		}

		/**
         * Returns the heartbeat interval.
         *
         * @return the heartbeat interval
         */
        public Duration getHeartbeatInterval() {
			return this.heartbeatInterval;
		}

		/**
         * Sets the heartbeat interval for the consumer.
         * 
         * @param heartbeatInterval the duration of the heartbeat interval
         */
        public void setHeartbeatInterval(Duration heartbeatInterval) {
			this.heartbeatInterval = heartbeatInterval;
		}

		/**
         * Returns the isolation level of the Consumer.
         *
         * @return the isolation level of the Consumer
         */
        public IsolationLevel getIsolationLevel() {
			return this.isolationLevel;
		}

		/**
         * Sets the isolation level for the consumer.
         * 
         * @param isolationLevel the isolation level to be set
         */
        public void setIsolationLevel(IsolationLevel isolationLevel) {
			this.isolationLevel = isolationLevel;
		}

		/**
         * Returns the key deserializer for the Consumer class.
         *
         * @return the key deserializer for the Consumer class
         */
        public Class<?> getKeyDeserializer() {
			return this.keyDeserializer;
		}

		/**
         * Sets the key deserializer for the Consumer.
         * 
         * @param keyDeserializer the class representing the key deserializer
         */
        public void setKeyDeserializer(Class<?> keyDeserializer) {
			this.keyDeserializer = keyDeserializer;
		}

		/**
         * Returns the value deserializer used by the consumer.
         *
         * @return the value deserializer used by the consumer
         */
        public Class<?> getValueDeserializer() {
			return this.valueDeserializer;
		}

		/**
         * Sets the value deserializer for the Consumer.
         * 
         * @param valueDeserializer the class representing the value deserializer
         */
        public void setValueDeserializer(Class<?> valueDeserializer) {
			this.valueDeserializer = valueDeserializer;
		}

		/**
         * Returns the maximum number of poll records that can be fetched by the consumer.
         *
         * @return the maximum number of poll records
         */
        public Integer getMaxPollRecords() {
			return this.maxPollRecords;
		}

		/**
         * Sets the maximum number of records to be polled by the consumer.
         * 
         * @param maxPollRecords the maximum number of records to be polled
         */
        public void setMaxPollRecords(Integer maxPollRecords) {
			this.maxPollRecords = maxPollRecords;
		}

		/**
         * Returns the properties of the Consumer.
         * 
         * @return a Map containing the properties of the Consumer
         */
        public Map<String, String> getProperties() {
			return this.properties;
		}

		/**
         * Builds a map of properties for the Consumer class.
         * 
         * @param sslBundles the SSL bundles to include in the properties
         * @return a map of properties for the Consumer class
         */
        public Map<String, Object> buildProperties(SslBundles sslBundles) {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getAutoCommitInterval)
				.asInt(Duration::toMillis)
				.to(properties.in(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
			map.from(this::getAutoOffsetReset).to(properties.in(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
			map.from(this::getBootstrapServers).to(properties.in(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this::getClientId).to(properties.in(ConsumerConfig.CLIENT_ID_CONFIG));
			map.from(this::getEnableAutoCommit).to(properties.in(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
			map.from(this::getFetchMaxWait)
				.asInt(Duration::toMillis)
				.to(properties.in(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG));
			map.from(this::getFetchMinSize)
				.asInt(DataSize::toBytes)
				.to(properties.in(ConsumerConfig.FETCH_MIN_BYTES_CONFIG));
			map.from(this::getGroupId).to(properties.in(ConsumerConfig.GROUP_ID_CONFIG));
			map.from(this::getHeartbeatInterval)
				.asInt(Duration::toMillis)
				.to(properties.in(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
			map.from(() -> getIsolationLevel().name().toLowerCase(Locale.ROOT))
				.to(properties.in(ConsumerConfig.ISOLATION_LEVEL_CONFIG));
			map.from(this::getKeyDeserializer).to(properties.in(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
			map.from(this::getValueDeserializer).to(properties.in(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
			map.from(this::getMaxPollRecords).to(properties.in(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
			return properties.with(this.ssl, this.security, this.properties, sslBundles);
		}

	}

	/**
     * Producer class.
     */
    public static class Producer {

		private final Ssl ssl = new Ssl();

		private final Security security = new Security();

		/**
		 * Number of acknowledgments the producer requires the leader to have received
		 * before considering a request complete.
		 */
		private String acks;

		/**
		 * Default batch size. A small batch size will make batching less common and may
		 * reduce throughput (a batch size of zero disables batching entirely).
		 */
		private DataSize batchSize;

		/**
		 * Comma-delimited list of host:port pairs to use for establishing the initial
		 * connections to the Kafka cluster. Overrides the global property, for producers.
		 */
		private List<String> bootstrapServers;

		/**
		 * Total memory size the producer can use to buffer records waiting to be sent to
		 * the server.
		 */
		private DataSize bufferMemory;

		/**
		 * ID to pass to the server when making requests. Used for server-side logging.
		 */
		private String clientId;

		/**
		 * Compression type for all data generated by the producer.
		 */
		private String compressionType;

		/**
		 * Serializer class for keys.
		 */
		private Class<?> keySerializer = StringSerializer.class;

		/**
		 * Serializer class for values.
		 */
		private Class<?> valueSerializer = StringSerializer.class;

		/**
		 * When greater than zero, enables retrying of failed sends.
		 */
		private Integer retries;

		/**
		 * When non empty, enables transaction support for producer.
		 */
		private String transactionIdPrefix;

		/**
		 * Additional producer-specific properties used to configure the client.
		 */
		private final Map<String, String> properties = new HashMap<>();

		/**
         * Returns the SSL object associated with this Producer.
         *
         * @return the SSL object associated with this Producer
         */
        public Ssl getSsl() {
			return this.ssl;
		}

		/**
         * Returns the security object associated with this Producer.
         *
         * @return the security object
         */
        public Security getSecurity() {
			return this.security;
		}

		/**
         * Returns the acknowledgements received by the producer.
         *
         * @return the acknowledgements received by the producer
         */
        public String getAcks() {
			return this.acks;
		}

		/**
         * Sets the acknowledgement mode for the producer.
         * 
         * @param acks the acknowledgement mode to be set
         */
        public void setAcks(String acks) {
			this.acks = acks;
		}

		/**
         * Returns the batch size used by the producer.
         *
         * @return the batch size used by the producer
         */
        public DataSize getBatchSize() {
			return this.batchSize;
		}

		/**
         * Sets the batch size for the producer.
         * 
         * @param batchSize the batch size to be set
         */
        public void setBatchSize(DataSize batchSize) {
			this.batchSize = batchSize;
		}

		/**
         * Returns the list of bootstrap servers.
         *
         * @return the list of bootstrap servers
         */
        public List<String> getBootstrapServers() {
			return this.bootstrapServers;
		}

		/**
         * Sets the list of bootstrap servers for the producer.
         * 
         * @param bootstrapServers the list of bootstrap servers to be set
         */
        public void setBootstrapServers(List<String> bootstrapServers) {
			this.bootstrapServers = bootstrapServers;
		}

		/**
         * Returns the buffer memory size.
         *
         * @return the buffer memory size
         */
        public DataSize getBufferMemory() {
			return this.bufferMemory;
		}

		/**
         * Sets the buffer memory size for the producer.
         * 
         * @param bufferMemory the buffer memory size to be set
         */
        public void setBufferMemory(DataSize bufferMemory) {
			this.bufferMemory = bufferMemory;
		}

		/**
         * Returns the client ID associated with this Producer.
         *
         * @return the client ID
         */
        public String getClientId() {
			return this.clientId;
		}

		/**
         * Sets the client ID for the Producer.
         * 
         * @param clientId the client ID to be set
         */
        public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		/**
         * Returns the compression type used by the producer.
         * 
         * @return the compression type
         */
        public String getCompressionType() {
			return this.compressionType;
		}

		/**
         * Sets the compression type for the producer.
         * 
         * @param compressionType the compression type to be set
         */
        public void setCompressionType(String compressionType) {
			this.compressionType = compressionType;
		}

		/**
         * Returns the key serializer used by the producer.
         *
         * @return the key serializer used by the producer
         */
        public Class<?> getKeySerializer() {
			return this.keySerializer;
		}

		/**
         * Sets the key serializer for the producer.
         * 
         * @param keySerializer the class representing the key serializer
         */
        public void setKeySerializer(Class<?> keySerializer) {
			this.keySerializer = keySerializer;
		}

		/**
         * Returns the value serializer used by the Producer.
         *
         * @return the value serializer used by the Producer
         */
        public Class<?> getValueSerializer() {
			return this.valueSerializer;
		}

		/**
         * Sets the value serializer for the Producer.
         * 
         * @param valueSerializer the class representing the value serializer to be used
         */
        public void setValueSerializer(Class<?> valueSerializer) {
			this.valueSerializer = valueSerializer;
		}

		/**
         * Returns the number of retries for the Producer.
         *
         * @return the number of retries
         */
        public Integer getRetries() {
			return this.retries;
		}

		/**
         * Sets the number of retries for the producer.
         * 
         * @param retries the number of retries to be set
         */
        public void setRetries(Integer retries) {
			this.retries = retries;
		}

		/**
         * Returns the transaction ID prefix.
         * 
         * @return the transaction ID prefix
         */
        public String getTransactionIdPrefix() {
			return this.transactionIdPrefix;
		}

		/**
         * Sets the prefix for the transaction ID.
         * 
         * @param transactionIdPrefix the prefix to be set for the transaction ID
         */
        public void setTransactionIdPrefix(String transactionIdPrefix) {
			this.transactionIdPrefix = transactionIdPrefix;
		}

		/**
         * Returns the properties of the Producer.
         * 
         * @return a Map containing the properties of the Producer
         */
        public Map<String, String> getProperties() {
			return this.properties;
		}

		/**
         * Builds a map of properties for the Producer class.
         * 
         * @param sslBundles the SSL bundles to be included in the properties
         * @return a map of properties for the Producer class
         */
        public Map<String, Object> buildProperties(SslBundles sslBundles) {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getAcks).to(properties.in(ProducerConfig.ACKS_CONFIG));
			map.from(this::getBatchSize).asInt(DataSize::toBytes).to(properties.in(ProducerConfig.BATCH_SIZE_CONFIG));
			map.from(this::getBootstrapServers).to(properties.in(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this::getBufferMemory)
				.as(DataSize::toBytes)
				.to(properties.in(ProducerConfig.BUFFER_MEMORY_CONFIG));
			map.from(this::getClientId).to(properties.in(ProducerConfig.CLIENT_ID_CONFIG));
			map.from(this::getCompressionType).to(properties.in(ProducerConfig.COMPRESSION_TYPE_CONFIG));
			map.from(this::getKeySerializer).to(properties.in(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
			map.from(this::getRetries).to(properties.in(ProducerConfig.RETRIES_CONFIG));
			map.from(this::getValueSerializer).to(properties.in(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
			return properties.with(this.ssl, this.security, this.properties, sslBundles);
		}

	}

	/**
     * Admin class.
     */
    public static class Admin {

		private final Ssl ssl = new Ssl();

		private final Security security = new Security();

		/**
		 * ID to pass to the server when making requests. Used for server-side logging.
		 */
		private String clientId;

		/**
		 * Additional admin-specific properties used to configure the client.
		 */
		private final Map<String, String> properties = new HashMap<>();

		/**
		 * Close timeout.
		 */
		private Duration closeTimeout;

		/**
		 * Operation timeout.
		 */
		private Duration operationTimeout;

		/**
		 * Whether to fail fast if the broker is not available on startup.
		 */
		private boolean failFast;

		/**
		 * Whether to enable modification of existing topic configuration.
		 */
		private boolean modifyTopicConfigs;

		/**
		 * Whether to automatically create topics during context initialization. When set
		 * to false, disables automatic topic creation during context initialization.
		 */
		private boolean autoCreate = true;

		/**
         * Returns the SSL object associated with this Admin instance.
         *
         * @return the SSL object
         */
        public Ssl getSsl() {
			return this.ssl;
		}

		/**
         * Returns the security object associated with this Admin.
         *
         * @return the security object
         */
        public Security getSecurity() {
			return this.security;
		}

		/**
         * Returns the client ID associated with the Admin.
         *
         * @return the client ID
         */
        public String getClientId() {
			return this.clientId;
		}

		/**
         * Sets the client ID for the Admin.
         * 
         * @param clientId the client ID to be set
         */
        public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		/**
         * Returns the close timeout duration.
         *
         * @return the close timeout duration
         */
        public Duration getCloseTimeout() {
			return this.closeTimeout;
		}

		/**
         * Sets the close timeout for the Admin class.
         * 
         * @param closeTimeout the duration of the close timeout
         */
        public void setCloseTimeout(Duration closeTimeout) {
			this.closeTimeout = closeTimeout;
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
         * Sets the operation timeout for the Admin class.
         * 
         * @param operationTimeout the duration of the operation timeout
         */
        public void setOperationTimeout(Duration operationTimeout) {
			this.operationTimeout = operationTimeout;
		}

		/**
         * Returns whether the fail-fast mode is enabled or not.
         * 
         * @return true if fail-fast mode is enabled, false otherwise
         */
        public boolean isFailFast() {
			return this.failFast;
		}

		/**
         * Sets the failFast flag.
         * 
         * @param failFast the value to set the failFast flag to
         */
        public void setFailFast(boolean failFast) {
			this.failFast = failFast;
		}

		/**
         * Returns a boolean value indicating whether the user has permission to modify topic configurations.
         * 
         * @return true if the user has permission to modify topic configurations, false otherwise
         */
        public boolean isModifyTopicConfigs() {
			return this.modifyTopicConfigs;
		}

		/**
         * Sets the flag indicating whether to modify topic configurations.
         * 
         * @param modifyTopicConfigs true to modify topic configurations, false otherwise
         */
        public void setModifyTopicConfigs(boolean modifyTopicConfigs) {
			this.modifyTopicConfigs = modifyTopicConfigs;
		}

		/**
         * Returns a boolean value indicating whether auto creation is enabled.
         * 
         * @return true if auto creation is enabled, false otherwise
         */
        public boolean isAutoCreate() {
			return this.autoCreate;
		}

		/**
         * Sets the autoCreate flag.
         * 
         * @param autoCreate the value to set for the autoCreate flag
         */
        public void setAutoCreate(boolean autoCreate) {
			this.autoCreate = autoCreate;
		}

		/**
         * Returns the properties of the Admin class.
         * 
         * @return a Map containing the properties of the Admin class, where the keys are strings and the values are strings
         */
        public Map<String, String> getProperties() {
			return this.properties;
		}

		/**
         * Builds a map of properties using the provided SslBundles.
         * 
         * @param sslBundles the SslBundles object containing SSL configurations
         * @return a map of properties
         */
        public Map<String, Object> buildProperties(SslBundles sslBundles) {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getClientId).to(properties.in(ProducerConfig.CLIENT_ID_CONFIG));
			return properties.with(this.ssl, this.security, this.properties, sslBundles);
		}

	}

	/**
	 * High (and some medium) priority Streams properties and a general properties bucket.
	 */
	public static class Streams {

		private final Ssl ssl = new Ssl();

		private final Security security = new Security();

		private final Cleanup cleanup = new Cleanup();

		/**
		 * Kafka streams application.id property; default spring.application.name.
		 */
		private String applicationId;

		/**
		 * Whether to auto-start the streams factory bean.
		 */
		private boolean autoStartup = true;

		/**
		 * Comma-delimited list of host:port pairs to use for establishing the initial
		 * connections to the Kafka cluster. Overrides the global property, for streams.
		 */
		private List<String> bootstrapServers;

		/**
		 * Maximum size of the in-memory state store cache across all threads.
		 */
		private DataSize stateStoreCacheMaxSize;

		/**
		 * ID to pass to the server when making requests. Used for server-side logging.
		 */
		private String clientId;

		/**
		 * The replication factor for change log topics and repartition topics created by
		 * the stream processing application.
		 */
		private Integer replicationFactor;

		/**
		 * Directory location for the state store.
		 */
		private String stateDir;

		/**
		 * Additional Kafka properties used to configure the streams.
		 */
		private final Map<String, String> properties = new HashMap<>();

		/**
         * Returns the SSL object associated with this Streams object.
         *
         * @return the SSL object associated with this Streams object
         */
        public Ssl getSsl() {
			return this.ssl;
		}

		/**
         * Returns the security object associated with this Streams object.
         *
         * @return the security object
         */
        public Security getSecurity() {
			return this.security;
		}

		/**
         * Returns the Cleanup object associated with this Streams object.
         *
         * @return the Cleanup object associated with this Streams object
         */
        public Cleanup getCleanup() {
			return this.cleanup;
		}

		/**
         * Returns the application ID.
         * 
         * @return the application ID
         */
        public String getApplicationId() {
			return this.applicationId;
		}

		/**
         * Sets the application ID for the Streams class.
         * 
         * @param applicationId the application ID to be set
         */
        public void setApplicationId(String applicationId) {
			this.applicationId = applicationId;
		}

		/**
         * Returns a boolean value indicating whether the auto startup feature is enabled.
         * 
         * @return true if auto startup is enabled, false otherwise
         */
        public boolean isAutoStartup() {
			return this.autoStartup;
		}

		/**
         * Sets the auto startup flag for the Streams class.
         * 
         * @param autoStartup the value to set for the auto startup flag
         */
        public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		/**
         * Returns the list of bootstrap servers.
         *
         * @return the list of bootstrap servers
         */
        public List<String> getBootstrapServers() {
			return this.bootstrapServers;
		}

		/**
         * Sets the list of bootstrap servers for the Streams class.
         * 
         * @param bootstrapServers the list of bootstrap servers to be set
         */
        public void setBootstrapServers(List<String> bootstrapServers) {
			this.bootstrapServers = bootstrapServers;
		}

		/**
         * Returns the maximum size of the state store cache.
         *
         * @return the maximum size of the state store cache
         */
        public DataSize getStateStoreCacheMaxSize() {
			return this.stateStoreCacheMaxSize;
		}

		/**
         * Sets the maximum size of the state store cache.
         * 
         * @param stateStoreCacheMaxSize the maximum size of the state store cache
         */
        public void setStateStoreCacheMaxSize(DataSize stateStoreCacheMaxSize) {
			this.stateStoreCacheMaxSize = stateStoreCacheMaxSize;
		}

		/**
         * Returns the client ID.
         *
         * @return the client ID
         */
        public String getClientId() {
			return this.clientId;
		}

		/**
         * Sets the client ID for the Streams class.
         * 
         * @param clientId the client ID to be set
         */
        public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		/**
         * Returns the replication factor of the Streams class.
         *
         * @return the replication factor of the Streams class
         */
        public Integer getReplicationFactor() {
			return this.replicationFactor;
		}

		/**
         * Sets the replication factor for the Streams class.
         * 
         * @param replicationFactor the replication factor to be set
         */
        public void setReplicationFactor(Integer replicationFactor) {
			this.replicationFactor = replicationFactor;
		}

		/**
         * Returns the state directory.
         * 
         * @return the state directory
         */
        public String getStateDir() {
			return this.stateDir;
		}

		/**
         * Sets the directory path for storing the state of the Streams.
         * 
         * @param stateDir the directory path to set
         */
        public void setStateDir(String stateDir) {
			this.stateDir = stateDir;
		}

		/**
         * Returns the properties of the Streams object.
         *
         * @return a Map containing the properties of the Streams object
         */
        public Map<String, String> getProperties() {
			return this.properties;
		}

		/**
         * Builds a map of properties based on the provided SslBundles.
         * 
         * @param sslBundles the SslBundles object containing SSL related information
         * @return a map of properties
         */
        public Map<String, Object> buildProperties(SslBundles sslBundles) {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getApplicationId).to(properties.in("application.id"));
			map.from(this::getBootstrapServers).to(properties.in(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this::getStateStoreCacheMaxSize)
				.asInt(DataSize::toBytes)
				.to(properties.in("statestore.cache.max.bytes"));
			map.from(this::getClientId).to(properties.in(CommonClientConfigs.CLIENT_ID_CONFIG));
			map.from(this::getReplicationFactor).to(properties.in("replication.factor"));
			map.from(this::getStateDir).to(properties.in("state.dir"));
			return properties.with(this.ssl, this.security, this.properties, sslBundles);
		}

	}

	/**
     * Template class.
     */
    public static class Template {

		/**
		 * Default topic to which messages are sent.
		 */
		private String defaultTopic;

		/**
		 * Transaction id prefix, override the transaction id prefix in the producer
		 * factory.
		 */
		private String transactionIdPrefix;

		/**
		 * Whether to enable observation.
		 */
		private boolean observationEnabled;

		/**
         * Returns the default topic.
         *
         * @return the default topic
         */
        public String getDefaultTopic() {
			return this.defaultTopic;
		}

		/**
         * Sets the default topic for the Template.
         * 
         * @param defaultTopic the default topic to be set
         */
        public void setDefaultTopic(String defaultTopic) {
			this.defaultTopic = defaultTopic;
		}

		/**
         * Returns the transaction ID prefix.
         * 
         * @return the transaction ID prefix
         */
        public String getTransactionIdPrefix() {
			return this.transactionIdPrefix;
		}

		/**
         * Sets the prefix for the transaction ID.
         * 
         * @param transactionIdPrefix the prefix to be set for the transaction ID
         */
        public void setTransactionIdPrefix(String transactionIdPrefix) {
			this.transactionIdPrefix = transactionIdPrefix;
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
         * Sets the observation enabled flag.
         * 
         * @param observationEnabled the flag indicating whether observation is enabled or not
         */
        public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

	}

	/**
     * Listener class.
     */
    public static class Listener {

		public enum Type {

			/**
			 * Invokes the endpoint with one ConsumerRecord at a time.
			 */
			SINGLE,

			/**
			 * Invokes the endpoint with a batch of ConsumerRecords.
			 */
			BATCH

		}

		/**
		 * Listener type.
		 */
		private Type type = Type.SINGLE;

		/**
		 * Listener AckMode. See the spring-kafka documentation.
		 */
		private AckMode ackMode;

		/**
		 * Support for asynchronous record acknowledgements. Only applies when
		 * spring.kafka.listener.ack-mode is manual or manual-immediate.
		 */
		private Boolean asyncAcks;

		/**
		 * Prefix for the listener's consumer client.id property.
		 */
		private String clientId;

		/**
		 * Number of threads to run in the listener containers.
		 */
		private Integer concurrency;

		/**
		 * Timeout to use when polling the consumer.
		 */
		private Duration pollTimeout;

		/**
		 * Multiplier applied to "pollTimeout" to determine if a consumer is
		 * non-responsive.
		 */
		private Float noPollThreshold;

		/**
		 * Number of records between offset commits when ackMode is "COUNT" or
		 * "COUNT_TIME".
		 */
		private Integer ackCount;

		/**
		 * Time between offset commits when ackMode is "TIME" or "COUNT_TIME".
		 */
		private Duration ackTime;

		/**
		 * Sleep interval between Consumer.poll(Duration) calls.
		 */
		private Duration idleBetweenPolls = Duration.ZERO;

		/**
		 * Time between publishing idle consumer events (no data received).
		 */
		private Duration idleEventInterval;

		/**
		 * Time between publishing idle partition consumer events (no data received for
		 * partition).
		 */
		private Duration idlePartitionEventInterval;

		/**
		 * Time between checks for non-responsive consumers. If a duration suffix is not
		 * specified, seconds will be used.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration monitorInterval;

		/**
		 * Whether to log the container configuration during initialization (INFO level).
		 */
		private Boolean logContainerConfig;

		/**
		 * Whether the container should fail to start if at least one of the configured
		 * topics are not present on the broker.
		 */
		private boolean missingTopicsFatal = false;

		/**
		 * Whether the container stops after the current record is processed or after all
		 * the records from the previous poll are processed.
		 */
		private boolean immediateStop = false;

		/**
		 * Whether to auto start the container.
		 */
		private boolean autoStartup = true;

		/**
		 * Whether to instruct the container to change the consumer thread name during
		 * initialization.
		 */
		private Boolean changeConsumerThreadName;

		/**
		 * Whether to enable observation.
		 */
		private boolean observationEnabled;

		/**
         * Returns the type of the listener.
         * 
         * @return the type of the listener
         */
        public Type getType() {
			return this.type;
		}

		/**
         * Sets the type of the listener.
         * 
         * @param type the type to be set
         */
        public void setType(Type type) {
			this.type = type;
		}

		/**
         * Returns the acknowledgement mode of the listener.
         * 
         * @return the acknowledgement mode of the listener
         */
        public AckMode getAckMode() {
			return this.ackMode;
		}

		/**
         * Sets the acknowledgement mode for the listener.
         * 
         * @param ackMode the acknowledgement mode to be set
         */
        public void setAckMode(AckMode ackMode) {
			this.ackMode = ackMode;
		}

		/**
         * Returns the value of the asyncAcks property.
         * 
         * @return the value of the asyncAcks property
         */
        public Boolean getAsyncAcks() {
			return this.asyncAcks;
		}

		/**
         * Sets the flag indicating whether asynchronous acknowledgements are enabled or not.
         * 
         * @param asyncAcks the flag indicating whether asynchronous acknowledgements are enabled or not
         */
        public void setAsyncAcks(Boolean asyncAcks) {
			this.asyncAcks = asyncAcks;
		}

		/**
         * Returns the client ID associated with this Listener.
         *
         * @return the client ID
         */
        public String getClientId() {
			return this.clientId;
		}

		/**
         * Sets the client ID.
         * 
         * @param clientId the client ID to be set
         */
        public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		/**
         * Returns the concurrency level of the listener.
         *
         * @return the concurrency level of the listener
         */
        public Integer getConcurrency() {
			return this.concurrency;
		}

		/**
         * Sets the concurrency level for the listener.
         * 
         * @param concurrency the concurrency level to be set
         */
        public void setConcurrency(Integer concurrency) {
			this.concurrency = concurrency;
		}

		/**
         * Returns the poll timeout duration.
         *
         * @return the poll timeout duration
         */
        public Duration getPollTimeout() {
			return this.pollTimeout;
		}

		/**
         * Sets the poll timeout for the listener.
         * 
         * @param pollTimeout the duration of the poll timeout
         */
        public void setPollTimeout(Duration pollTimeout) {
			this.pollTimeout = pollTimeout;
		}

		/**
         * Returns the value of the noPollThreshold.
         * 
         * @return the value of the noPollThreshold
         */
        public Float getNoPollThreshold() {
			return this.noPollThreshold;
		}

		/**
         * Sets the threshold value for no poll.
         * 
         * @param noPollThreshold the threshold value for no poll
         */
        public void setNoPollThreshold(Float noPollThreshold) {
			this.noPollThreshold = noPollThreshold;
		}

		/**
         * Returns the count of acknowledgements.
         *
         * @return the count of acknowledgements
         */
        public Integer getAckCount() {
			return this.ackCount;
		}

		/**
         * Sets the acknowledgement count.
         * 
         * @param ackCount the acknowledgement count to be set
         */
        public void setAckCount(Integer ackCount) {
			this.ackCount = ackCount;
		}

		/**
         * Returns the acknowledgement time.
         * 
         * @return the acknowledgement time as a Duration object
         */
        public Duration getAckTime() {
			return this.ackTime;
		}

		/**
         * Sets the acknowledgement time for the listener.
         * 
         * @param ackTime the acknowledgement time to be set
         */
        public void setAckTime(Duration ackTime) {
			this.ackTime = ackTime;
		}

		/**
         * Returns the duration of idle time between polls.
         *
         * @return the duration of idle time between polls
         */
        public Duration getIdleBetweenPolls() {
			return this.idleBetweenPolls;
		}

		/**
         * Sets the duration of idle time between polls.
         * 
         * @param idleBetweenPolls the duration of idle time between polls
         */
        public void setIdleBetweenPolls(Duration idleBetweenPolls) {
			this.idleBetweenPolls = idleBetweenPolls;
		}

		/**
         * Returns the idle event interval.
         * 
         * @return the idle event interval
         */
        public Duration getIdleEventInterval() {
			return this.idleEventInterval;
		}

		/**
         * Sets the interval at which idle events are triggered.
         * 
         * @param idleEventInterval the duration between each idle event
         */
        public void setIdleEventInterval(Duration idleEventInterval) {
			this.idleEventInterval = idleEventInterval;
		}

		/**
         * Returns the interval between idle partition events.
         *
         * @return the interval between idle partition events
         */
        public Duration getIdlePartitionEventInterval() {
			return this.idlePartitionEventInterval;
		}

		/**
         * Sets the interval for idle partition events.
         * 
         * @param idlePartitionEventInterval the duration of the interval for idle partition events
         */
        public void setIdlePartitionEventInterval(Duration idlePartitionEventInterval) {
			this.idlePartitionEventInterval = idlePartitionEventInterval;
		}

		/**
         * Returns the monitor interval.
         *
         * @return the monitor interval
         */
        public Duration getMonitorInterval() {
			return this.monitorInterval;
		}

		/**
         * Sets the monitor interval for the listener.
         * 
         * @param monitorInterval the duration of the monitor interval
         */
        public void setMonitorInterval(Duration monitorInterval) {
			this.monitorInterval = monitorInterval;
		}

		/**
         * Returns the value of the logContainerConfig property.
         * 
         * @return the value of the logContainerConfig property
         */
        public Boolean getLogContainerConfig() {
			return this.logContainerConfig;
		}

		/**
         * Sets the flag to enable or disable logging of container configuration.
         * 
         * @param logContainerConfig the flag indicating whether to log container configuration or not
         */
        public void setLogContainerConfig(Boolean logContainerConfig) {
			this.logContainerConfig = logContainerConfig;
		}

		/**
         * Returns a boolean value indicating whether missing topics are considered fatal.
         * 
         * @return true if missing topics are considered fatal, false otherwise
         */
        public boolean isMissingTopicsFatal() {
			return this.missingTopicsFatal;
		}

		/**
         * Sets whether missing topics should be treated as fatal errors.
         * 
         * @param missingTopicsFatal true if missing topics should be treated as fatal errors, false otherwise
         */
        public void setMissingTopicsFatal(boolean missingTopicsFatal) {
			this.missingTopicsFatal = missingTopicsFatal;
		}

		/**
         * Returns a boolean value indicating whether an immediate stop is requested.
         * 
         * @return true if an immediate stop is requested, false otherwise
         */
        public boolean isImmediateStop() {
			return this.immediateStop;
		}

		/**
         * Sets the flag to immediately stop the listener.
         * 
         * @param immediateStop true to immediately stop the listener, false otherwise
         */
        public void setImmediateStop(boolean immediateStop) {
			this.immediateStop = immediateStop;
		}

		/**
         * Returns a boolean value indicating whether the listener is set to automatically start.
         * 
         * @return true if the listener is set to automatically start, false otherwise
         */
        public boolean isAutoStartup() {
			return this.autoStartup;
		}

		/**
         * Sets the auto startup flag for the listener.
         * 
         * @param autoStartup the auto startup flag to set
         */
        public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		/**
         * Returns the value of the changeConsumerThreadName property.
         * 
         * @return the value of the changeConsumerThreadName property
         */
        public Boolean getChangeConsumerThreadName() {
			return this.changeConsumerThreadName;
		}

		/**
         * Sets the flag indicating whether to change the consumer thread name.
         * 
         * @param changeConsumerThreadName the flag indicating whether to change the consumer thread name
         */
        public void setChangeConsumerThreadName(Boolean changeConsumerThreadName) {
			this.changeConsumerThreadName = changeConsumerThreadName;
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
     * Ssl class.
     */
    public static class Ssl {

		/**
		 * Name of the SSL bundle to use.
		 */
		private String bundle;

		/**
		 * Password of the private key in either key store key or key store file.
		 */
		private String keyPassword;

		/**
		 * Certificate chain in PEM format with a list of X.509 certificates.
		 */
		private String keyStoreCertificateChain;

		/**
		 * Private key in PEM format with PKCS#8 keys.
		 */
		private String keyStoreKey;

		/**
		 * Location of the key store file.
		 */
		private Resource keyStoreLocation;

		/**
		 * Store password for the key store file.
		 */
		private String keyStorePassword;

		/**
		 * Type of the key store.
		 */
		private String keyStoreType;

		/**
		 * Trusted certificates in PEM format with X.509 certificates.
		 */
		private String trustStoreCertificates;

		/**
		 * Location of the trust store file.
		 */
		private Resource trustStoreLocation;

		/**
		 * Store password for the trust store file.
		 */
		private String trustStorePassword;

		/**
		 * Type of the trust store.
		 */
		private String trustStoreType;

		/**
		 * SSL protocol to use.
		 */
		private String protocol;

		/**
         * Returns the bundle associated with this Ssl object.
         * 
         * @return the bundle associated with this Ssl object
         */
        public String getBundle() {
			return this.bundle;
		}

		/**
         * Sets the bundle for the Ssl class.
         * 
         * @param bundle the bundle to be set
         */
        public void setBundle(String bundle) {
			this.bundle = bundle;
		}

		/**
         * Returns the key password used for SSL encryption.
         * 
         * @return the key password used for SSL encryption
         */
        public String getKeyPassword() {
			return this.keyPassword;
		}

		/**
         * Sets the password for the key used in SSL communication.
         * 
         * @param keyPassword the password for the key
         */
        public void setKeyPassword(String keyPassword) {
			this.keyPassword = keyPassword;
		}

		/**
         * Returns the certificate chain stored in the key store.
         * 
         * @return the certificate chain stored in the key store
         */
        public String getKeyStoreCertificateChain() {
			return this.keyStoreCertificateChain;
		}

		/**
         * Sets the key store certificate chain.
         * 
         * @param keyStoreCertificateChain the key store certificate chain to be set
         */
        public void setKeyStoreCertificateChain(String keyStoreCertificateChain) {
			this.keyStoreCertificateChain = keyStoreCertificateChain;
		}

		/**
         * Returns the key store key.
         * 
         * @return the key store key
         */
        public String getKeyStoreKey() {
			return this.keyStoreKey;
		}

		/**
         * Sets the key store key for SSL configuration.
         * 
         * @param keyStoreKey the key store key to be set
         */
        public void setKeyStoreKey(String keyStoreKey) {
			this.keyStoreKey = keyStoreKey;
		}

		/**
         * Returns the location of the keystore.
         *
         * @return the location of the keystore
         */
        public Resource getKeyStoreLocation() {
			return this.keyStoreLocation;
		}

		/**
         * Sets the location of the keystore file.
         * 
         * @param keyStoreLocation the location of the keystore file
         */
        public void setKeyStoreLocation(Resource keyStoreLocation) {
			this.keyStoreLocation = keyStoreLocation;
		}

		/**
         * Returns the password for the keystore.
         *
         * @return the password for the keystore
         */
        public String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		/**
         * Sets the password for the key store.
         * 
         * @param keyStorePassword the password for the key store
         */
        public void setKeyStorePassword(String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

		/**
         * Returns the type of the keystore used for SSL configuration.
         * 
         * @return the keystore type
         */
        public String getKeyStoreType() {
			return this.keyStoreType;
		}

		/**
         * Sets the type of the keystore.
         * 
         * @param keyStoreType the type of the keystore
         */
        public void setKeyStoreType(String keyStoreType) {
			this.keyStoreType = keyStoreType;
		}

		/**
         * Returns the trust store certificates.
         * 
         * @return the trust store certificates
         */
        public String getTrustStoreCertificates() {
			return this.trustStoreCertificates;
		}

		/**
         * Sets the trust store certificates for SSL connections.
         * 
         * @param trustStoreCertificates the trust store certificates to be set
         */
        public void setTrustStoreCertificates(String trustStoreCertificates) {
			this.trustStoreCertificates = trustStoreCertificates;
		}

		/**
         * Returns the location of the trust store.
         *
         * @return the location of the trust store
         */
        public Resource getTrustStoreLocation() {
			return this.trustStoreLocation;
		}

		/**
         * Sets the location of the trust store file.
         * 
         * @param trustStoreLocation the location of the trust store file
         */
        public void setTrustStoreLocation(Resource trustStoreLocation) {
			this.trustStoreLocation = trustStoreLocation;
		}

		/**
         * Returns the password for the trust store.
         *
         * @return the trust store password
         */
        public String getTrustStorePassword() {
			return this.trustStorePassword;
		}

		/**
         * Sets the password for the trust store.
         * 
         * @param trustStorePassword the password for the trust store
         */
        public void setTrustStorePassword(String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
		}

		/**
         * Returns the type of the trust store.
         * 
         * @return the trust store type
         */
        public String getTrustStoreType() {
			return this.trustStoreType;
		}

		/**
         * Sets the type of the trust store.
         * 
         * @param trustStoreType the type of the trust store
         */
        public void setTrustStoreType(String trustStoreType) {
			this.trustStoreType = trustStoreType;
		}

		/**
         * Returns the protocol used by the SSL connection.
         *
         * @return the protocol used by the SSL connection
         */
        public String getProtocol() {
			return this.protocol;
		}

		/**
         * Sets the protocol for the SSL connection.
         * 
         * @param protocol the protocol to be set
         */
        public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		/**
         * Builds the properties for SSL configuration.
         * 
         * @return a map of SSL properties
         * 
         * @deprecated This method has been deprecated since version 3.2.0 and will be removed in a future release.
         *             Please use the {@link #buildProperties(Map)} method instead.
         */
        @Deprecated(since = "3.2.0", forRemoval = true)
		public Map<String, Object> buildProperties() {
			return buildProperties(null);
		}

		/**
         * Builds a map of properties based on the provided SslBundles object.
         * 
         * @param sslBundles The SslBundles object containing SSL bundle information.
         * @return A map of properties.
         */
        public Map<String, Object> buildProperties(SslBundles sslBundles) {
			validate();
			Properties properties = new Properties();
			if (getBundle() != null) {
				properties.in(SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG)
					.accept(SslBundleSslEngineFactory.class.getName());
				properties.in(SslBundle.class.getName()).accept(sslBundles.getBundle(getBundle()));
			}
			else {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getKeyPassword).to(properties.in(SslConfigs.SSL_KEY_PASSWORD_CONFIG));
				map.from(this::getKeyStoreCertificateChain)
					.to(properties.in(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG));
				map.from(this::getKeyStoreKey).to(properties.in(SslConfigs.SSL_KEYSTORE_KEY_CONFIG));
				map.from(this::getKeyStoreLocation)
					.as(this::resourceToPath)
					.to(properties.in(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG));
				map.from(this::getKeyStorePassword).to(properties.in(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG));
				map.from(this::getKeyStoreType).to(properties.in(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG));
				map.from(this::getTrustStoreCertificates)
					.to(properties.in(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG));
				map.from(this::getTrustStoreLocation)
					.as(this::resourceToPath)
					.to(properties.in(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
				map.from(this::getTrustStorePassword).to(properties.in(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
				map.from(this::getTrustStoreType).to(properties.in(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG));
				map.from(this::getProtocol).to(properties.in(SslConfigs.SSL_PROTOCOL_CONFIG));
			}
			return properties;
		}

		/**
         * Validates the SSL configuration properties.
         * 
         * @throws MutuallyExclusiveConfigurationPropertiesException if multiple non-null values are found in the SSL configuration properties
         */
        private void validate() {
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("spring.kafka.ssl.key-store-key", getKeyStoreKey());
				entries.put("spring.kafka.ssl.key-store-location", getKeyStoreLocation());
			});
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("spring.kafka.ssl.trust-store-certificates", getTrustStoreCertificates());
				entries.put("spring.kafka.ssl.trust-store-location", getTrustStoreLocation());
			});
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("spring.kafka.ssl.bundle", getBundle());
				entries.put("spring.kafka.ssl.key-store-key", getKeyStoreKey());
			});
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("spring.kafka.ssl.bundle", getBundle());
				entries.put("spring.kafka.ssl.key-store-location", getKeyStoreLocation());
			});
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("spring.kafka.ssl.bundle", getBundle());
				entries.put("spring.kafka.ssl.trust-store-certificates", getTrustStoreCertificates());
			});
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("spring.kafka.ssl.bundle", getBundle());
				entries.put("spring.kafka.ssl.trust-store-location", getTrustStoreLocation());
			});
		}

		/**
         * Converts a resource to its absolute file path.
         * 
         * @param resource the resource to convert
         * @return the absolute file path of the resource
         * @throws IllegalStateException if the resource is not on a file system
         */
        private String resourceToPath(Resource resource) {
			try {
				return resource.getFile().getAbsolutePath();
			}
			catch (IOException ex) {
				throw new IllegalStateException("Resource '" + resource + "' must be on a file system", ex);
			}
		}

	}

	/**
     * Jaas class.
     */
    public static class Jaas {

		/**
		 * Whether to enable JAAS configuration.
		 */
		private boolean enabled;

		/**
		 * Login module.
		 */
		private String loginModule = "com.sun.security.auth.module.Krb5LoginModule";

		/**
		 * Control flag for login configuration.
		 */
		private KafkaJaasLoginModuleInitializer.ControlFlag controlFlag = KafkaJaasLoginModuleInitializer.ControlFlag.REQUIRED;

		/**
		 * Additional JAAS options.
		 */
		private final Map<String, String> options = new HashMap<>();

		/**
         * Returns the current status of the enabled flag.
         *
         * @return true if the flag is enabled, false otherwise.
         */
        public boolean isEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the Jaas object.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
         * Returns the login module.
         * 
         * @return the login module
         */
        public String getLoginModule() {
			return this.loginModule;
		}

		/**
         * Sets the login module for the JAAS class.
         * 
         * @param loginModule the name of the login module to be set
         */
        public void setLoginModule(String loginModule) {
			this.loginModule = loginModule;
		}

		/**
         * Returns the control flag of the KafkaJaasLoginModuleInitializer.
         *
         * @return the control flag of the KafkaJaasLoginModuleInitializer
         */
        public KafkaJaasLoginModuleInitializer.ControlFlag getControlFlag() {
			return this.controlFlag;
		}

		/**
         * Sets the control flag for the KafkaJaasLoginModuleInitializer.
         * 
         * @param controlFlag the control flag to be set
         */
        public void setControlFlag(KafkaJaasLoginModuleInitializer.ControlFlag controlFlag) {
			this.controlFlag = controlFlag;
		}

		/**
         * Returns the options of the Jaas class.
         * 
         * @return a Map containing the options as key-value pairs
         */
        public Map<String, String> getOptions() {
			return this.options;
		}

		/**
         * Sets the options for the Jaas class.
         * 
         * @param options a map containing the options to be set
         */
        public void setOptions(Map<String, String> options) {
			if (options != null) {
				this.options.putAll(options);
			}
		}

	}

	/**
     * Security class.
     */
    public static class Security {

		/**
		 * Security protocol used to communicate with brokers.
		 */
		private String protocol;

		/**
         * Returns the protocol used by the Security class.
         *
         * @return the protocol used by the Security class
         */
        public String getProtocol() {
			return this.protocol;
		}

		/**
         * Sets the protocol for the security.
         * 
         * @param protocol the protocol to be set
         */
        public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		/**
         * Builds a map of properties for the Security class.
         * 
         * @return a map of properties
         */
        public Map<String, Object> buildProperties() {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getProtocol).to(properties.in(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
			return properties;
		}

	}

	/**
     * Retry class.
     */
    public static class Retry {

		private final Topic topic = new Topic();

		/**
         * Returns the topic of the Retry object.
         *
         * @return the topic of the Retry object
         */
        public Topic getTopic() {
			return this.topic;
		}

		/**
		 * Properties for non-blocking, topic-based retries.
		 */
		public static class Topic {

			/**
			 * Whether to enable topic-based non-blocking retries.
			 */
			private boolean enabled;

			/**
			 * Total number of processing attempts made before sending the message to the
			 * DLT.
			 */
			private int attempts = 3;

			/**
			 * Canonical backoff period. Used as an initial value in the exponential case,
			 * and as a minimum value in the uniform case.
			 */
			private Duration delay = Duration.ofSeconds(1);

			/**
			 * Multiplier to use for generating the next backoff delay.
			 */
			private double multiplier = 0.0;

			/**
			 * Maximum wait between retries. If less than the delay then the default of 30
			 * seconds is applied.
			 */
			private Duration maxDelay = Duration.ZERO;

			/**
			 * Whether to have the backoff delays.
			 */
			private boolean randomBackOff = false;

			/**
             * Returns the current status of the topic's enabled flag.
             * 
             * @return true if the topic is enabled, false otherwise
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the Topic.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			/**
             * Returns the number of attempts made.
             *
             * @return the number of attempts made
             */
            public int getAttempts() {
				return this.attempts;
			}

			/**
             * Sets the number of attempts made.
             * 
             * @param attempts the number of attempts made
             */
            public void setAttempts(int attempts) {
				this.attempts = attempts;
			}

			/**
             * Returns the delay of the topic.
             *
             * @return the delay of the topic
             */
            public Duration getDelay() {
				return this.delay;
			}

			/**
             * Sets the delay for the Topic.
             * 
             * @param delay the duration of the delay
             */
            public void setDelay(Duration delay) {
				this.delay = delay;
			}

			/**
             * Returns the multiplier value.
             * 
             * @return the multiplier value
             */
            public double getMultiplier() {
				return this.multiplier;
			}

			/**
             * Sets the multiplier for the Topic.
             * 
             * @param multiplier the multiplier value to be set
             */
            public void setMultiplier(double multiplier) {
				this.multiplier = multiplier;
			}

			/**
             * Returns the maximum delay.
             *
             * @return the maximum delay
             */
            public Duration getMaxDelay() {
				return this.maxDelay;
			}

			/**
             * Sets the maximum delay for the topic.
             * 
             * @param maxDelay the maximum delay to be set
             */
            public void setMaxDelay(Duration maxDelay) {
				this.maxDelay = maxDelay;
			}

			/**
             * Returns a boolean value indicating whether random backoff is enabled.
             * 
             * @return true if random backoff is enabled, false otherwise
             */
            public boolean isRandomBackOff() {
				return this.randomBackOff;
			}

			/**
             * Sets the flag indicating whether to use random back off.
             * 
             * @param randomBackOff the flag indicating whether to use random back off
             */
            public void setRandomBackOff(boolean randomBackOff) {
				this.randomBackOff = randomBackOff;
			}

		}

	}

	/**
     * Cleanup class.
     */
    public static class Cleanup {

		/**
		 * Cleanup the application’s local state directory on startup.
		 */
		private boolean onStartup = false;

		/**
		 * Cleanup the application’s local state directory on shutdown.
		 */
		private boolean onShutdown = false;

		/**
         * Returns a boolean value indicating whether the software is on startup.
         * 
         * @return true if the software is on startup, false otherwise
         */
        public boolean isOnStartup() {
			return this.onStartup;
		}

		/**
         * Sets the value indicating whether the cleanup process should run on startup.
         * 
         * @param onStartup the value indicating whether the cleanup process should run on startup
         */
        public void setOnStartup(boolean onStartup) {
			this.onStartup = onStartup;
		}

		/**
         * Returns a boolean value indicating whether the system is on shutdown.
         * 
         * @return true if the system is on shutdown, false otherwise
         */
        public boolean isOnShutdown() {
			return this.onShutdown;
		}

		/**
         * Sets the value indicating whether the shutdown event has occurred.
         * 
         * @param onShutdown the value indicating whether the shutdown event has occurred
         */
        public void setOnShutdown(boolean onShutdown) {
			this.onShutdown = onShutdown;
		}

	}

	public enum IsolationLevel {

		/**
		 * Read everything including aborted transactions.
		 */
		READ_UNCOMMITTED((byte) 0),

		/**
		 * Read records from committed transactions, in addition to records not part of
		 * transactions.
		 */
		READ_COMMITTED((byte) 1);

		private final byte id;

		/**
     * Constructs a new IsolationLevel object with the specified ID.
     *
     * @param id the ID of the isolation level
     */
    IsolationLevel(byte id) {
			this.id = id;
		}

		/**
     * Returns the ID of the KafkaProperties object.
     *
     * @return the ID of the KafkaProperties object
     */
    public byte id() {
			return this.id;
		}

	}

	/**
     * Properties class.
     */
    @SuppressWarnings("serial")
	private static final class Properties extends HashMap<String, Object> {

		/**
         * Associates the specified value with the specified key in this Properties object.
         * 
         * @param key the key with which the specified value is to be associated
         * @return a Consumer that accepts a value and puts it into this Properties object with the specified key
         */
        <V> java.util.function.Consumer<V> in(String key) {
			return (value) -> put(key, value);
		}

		/**
         * Adds SSL, security, and custom properties to the current properties object.
         * 
         * @param ssl         the SSL object used to build SSL properties
         * @param security    the Security object used to build security properties
         * @param properties  a map of custom properties to be added
         * @param sslBundles  the SSLBundles object used to build SSL properties
         * @return the updated properties object
         */
        Properties with(Ssl ssl, Security security, Map<String, String> properties, SslBundles sslBundles) {
			putAll(ssl.buildProperties(sslBundles));
			putAll(security.buildProperties());
			putAll(properties);
			return this;
		}

	}

}
