/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.core.io.Resource;
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.util.CollectionUtils;

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

	private final Listener listener = new Listener();

	private final Ssl ssl = new Ssl();

	private final Jaas jaas = new Jaas();

	private final Template template = new Template();

	public List<String> getBootstrapServers() {
		return this.bootstrapServers;
	}

	public void setBootstrapServers(List<String> bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public Consumer getConsumer() {
		return this.consumer;
	}

	public Producer getProducer() {
		return this.producer;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Admin getAdmin() {
		return this.admin;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public Jaas getJaas() {
		return this.jaas;
	}

	public Template getTemplate() {
		return this.template;
	}

	private Map<String, Object> buildCommonProperties() {
		Map<String, Object> properties = new HashMap<>();
		if (this.bootstrapServers != null) {
			properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		}
		if (this.clientId != null) {
			properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, this.clientId);
		}
		properties.putAll(this.ssl.buildProperties());
		if (!CollectionUtils.isEmpty(this.properties)) {
			properties.putAll(this.properties);
		}
		return properties;
	}

	/**
	 * Create an initial map of consumer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default kafkaConsumerFactory bean.
	 * @return the consumer properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildConsumerProperties() {
		Map<String, Object> properties = buildCommonProperties();
		properties.putAll(this.consumer.buildProperties());
		return properties;
	}

	/**
	 * Create an initial map of producer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default kafkaProducerFactory bean.
	 * @return the producer properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildProducerProperties() {
		Map<String, Object> properties = buildCommonProperties();
		properties.putAll(this.producer.buildProperties());
		return properties;
	}

	/**
	 * Create an initial map of admin properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default kafkaAdmin bean.
	 * @return the admin properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildAdminProperties() {
		Map<String, Object> properties = buildCommonProperties();
		properties.putAll(this.admin.buildProperties());
		return properties;
	}

	public static class Consumer {

		private final Ssl ssl = new Ssl();

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
		 * "fetch.min.bytes".
		 */
		private Duration fetchMaxWait;

		/**
		 * Minimum amount of data, in bytes, the server should return for a fetch request.
		 */
		private Integer fetchMinSize;

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

		public Ssl getSsl() {
			return this.ssl;
		}

		public Duration getAutoCommitInterval() {
			return this.autoCommitInterval;
		}

		public void setAutoCommitInterval(Duration autoCommitInterval) {
			this.autoCommitInterval = autoCommitInterval;
		}

		public String getAutoOffsetReset() {
			return this.autoOffsetReset;
		}

		public void setAutoOffsetReset(String autoOffsetReset) {
			this.autoOffsetReset = autoOffsetReset;
		}

		public List<String> getBootstrapServers() {
			return this.bootstrapServers;
		}

		public void setBootstrapServers(List<String> bootstrapServers) {
			this.bootstrapServers = bootstrapServers;
		}

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public Boolean getEnableAutoCommit() {
			return this.enableAutoCommit;
		}

		public void setEnableAutoCommit(Boolean enableAutoCommit) {
			this.enableAutoCommit = enableAutoCommit;
		}

		public Duration getFetchMaxWait() {
			return this.fetchMaxWait;
		}

		public void setFetchMaxWait(Duration fetchMaxWait) {
			this.fetchMaxWait = fetchMaxWait;
		}

		public Integer getFetchMinSize() {
			return this.fetchMinSize;
		}

		public void setFetchMinSize(Integer fetchMinSize) {
			this.fetchMinSize = fetchMinSize;
		}

		public String getGroupId() {
			return this.groupId;
		}

		public void setGroupId(String groupId) {
			this.groupId = groupId;
		}

		public Duration getHeartbeatInterval() {
			return this.heartbeatInterval;
		}

		public void setHeartbeatInterval(Duration heartbeatInterval) {
			this.heartbeatInterval = heartbeatInterval;
		}

		public Class<?> getKeyDeserializer() {
			return this.keyDeserializer;
		}

		public void setKeyDeserializer(Class<?> keyDeserializer) {
			this.keyDeserializer = keyDeserializer;
		}

		public Class<?> getValueDeserializer() {
			return this.valueDeserializer;
		}

		public void setValueDeserializer(Class<?> valueDeserializer) {
			this.valueDeserializer = valueDeserializer;
		}

		public Integer getMaxPollRecords() {
			return this.maxPollRecords;
		}

		public void setMaxPollRecords(Integer maxPollRecords) {
			this.maxPollRecords = maxPollRecords;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public Map<String, Object> buildProperties() {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getAutoCommitInterval).asInt(Duration::toMillis)
					.to(properties.in(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
			map.from(this::getAutoOffsetReset).to(properties.in(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
			map.from(this::getBootstrapServers).to(properties.in(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this::getClientId).to(properties.in(ConsumerConfig.CLIENT_ID_CONFIG));
			map.from(this::getEnableAutoCommit).to(properties.in(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
			map.from(this::getFetchMaxWait).asInt(Duration::toMillis)
					.to(properties.in(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG));
			map.from(this::getFetchMinSize).to(properties.in(ConsumerConfig.FETCH_MIN_BYTES_CONFIG));
			map.from(this::getGroupId).to(properties.in(ConsumerConfig.GROUP_ID_CONFIG));
			map.from(this::getHeartbeatInterval).asInt(Duration::toMillis)
					.to(properties.in(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
			map.from(this::getKeyDeserializer).to(properties.in(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
			map.from(this::getValueDeserializer).to(properties.in(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
			map.from(this::getMaxPollRecords).to(properties.in(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
			return properties.with(this.ssl, this.properties);
		}

	}

	public static class Producer {

		private final Ssl ssl = new Ssl();

		/**
		 * Number of acknowledgments the producer requires the leader to have received
		 * before considering a request complete.
		 */
		private String acks;

		/**
		 * Default batch size in bytes. A small batch size will make batching less common
		 * and may reduce throughput (a batch size of zero disables batching entirely).
		 */
		private Integer batchSize;

		/**
		 * Comma-delimited list of host:port pairs to use for establishing the initial
		 * connections to the Kafka cluster. Overrides the global property, for producers.
		 */
		private List<String> bootstrapServers;

		/**
		 * Total bytes of memory the producer can use to buffer records waiting to be sent
		 * to the server.
		 */
		private Long bufferMemory;

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

		public Ssl getSsl() {
			return this.ssl;
		}

		public String getAcks() {
			return this.acks;
		}

		public void setAcks(String acks) {
			this.acks = acks;
		}

		public Integer getBatchSize() {
			return this.batchSize;
		}

		public void setBatchSize(Integer batchSize) {
			this.batchSize = batchSize;
		}

		public List<String> getBootstrapServers() {
			return this.bootstrapServers;
		}

		public void setBootstrapServers(List<String> bootstrapServers) {
			this.bootstrapServers = bootstrapServers;
		}

		public Long getBufferMemory() {
			return this.bufferMemory;
		}

		public void setBufferMemory(Long bufferMemory) {
			this.bufferMemory = bufferMemory;
		}

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getCompressionType() {
			return this.compressionType;
		}

		public void setCompressionType(String compressionType) {
			this.compressionType = compressionType;
		}

		public Class<?> getKeySerializer() {
			return this.keySerializer;
		}

		public void setKeySerializer(Class<?> keySerializer) {
			this.keySerializer = keySerializer;
		}

		public Class<?> getValueSerializer() {
			return this.valueSerializer;
		}

		public void setValueSerializer(Class<?> valueSerializer) {
			this.valueSerializer = valueSerializer;
		}

		public Integer getRetries() {
			return this.retries;
		}

		public void setRetries(Integer retries) {
			this.retries = retries;
		}

		public String getTransactionIdPrefix() {
			return this.transactionIdPrefix;
		}

		public void setTransactionIdPrefix(String transactionIdPrefix) {
			this.transactionIdPrefix = transactionIdPrefix;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public Map<String, Object> buildProperties() {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getAcks).to(properties.in(ProducerConfig.ACKS_CONFIG));
			map.from(this::getBatchSize).to(properties.in(ProducerConfig.BATCH_SIZE_CONFIG));
			map.from(this::getBootstrapServers).to(properties.in(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this::getBufferMemory).to(properties.in(ProducerConfig.BUFFER_MEMORY_CONFIG));
			map.from(this::getClientId).to(properties.in(ProducerConfig.CLIENT_ID_CONFIG));
			map.from(this::getCompressionType).to(properties.in(ProducerConfig.COMPRESSION_TYPE_CONFIG));
			map.from(this::getKeySerializer).to(properties.in(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
			map.from(this::getRetries).to(properties.in(ProducerConfig.RETRIES_CONFIG));
			map.from(this::getValueSerializer).to(properties.in(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
			return properties.with(this.ssl, this.properties);
		}

	}

	public static class Admin {

		private final Ssl ssl = new Ssl();

		/**
		 * ID to pass to the server when making requests. Used for server-side logging.
		 */
		private String clientId;

		/**
		 * Additional admin-specific properties used to configure the client.
		 */
		private final Map<String, String> properties = new HashMap<>();

		/**
		 * Whether to fail fast if the broker is not available on startup.
		 */
		private boolean failFast;

		public Ssl getSsl() {
			return this.ssl;
		}

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public boolean isFailFast() {
			return this.failFast;
		}

		public void setFailFast(boolean failFast) {
			this.failFast = failFast;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

		public Map<String, Object> buildProperties() {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getClientId).to(properties.in(ProducerConfig.CLIENT_ID_CONFIG));
			return properties.with(this.ssl, this.properties);
		}

	}

	public static class Template {

		/**
		 * Default topic to which messages are sent.
		 */
		private String defaultTopic;

		public String getDefaultTopic() {
			return this.defaultTopic;
		}

		public void setDefaultTopic(String defaultTopic) {
			this.defaultTopic = defaultTopic;
		}

	}

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
		 * Time between publishing idle consumer events (no data received).
		 */
		private Duration idleEventInterval;

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

		public Type getType() {
			return this.type;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public AckMode getAckMode() {
			return this.ackMode;
		}

		public void setAckMode(AckMode ackMode) {
			this.ackMode = ackMode;
		}

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public Integer getConcurrency() {
			return this.concurrency;
		}

		public void setConcurrency(Integer concurrency) {
			this.concurrency = concurrency;
		}

		public Duration getPollTimeout() {
			return this.pollTimeout;
		}

		public void setPollTimeout(Duration pollTimeout) {
			this.pollTimeout = pollTimeout;
		}

		public Float getNoPollThreshold() {
			return this.noPollThreshold;
		}

		public void setNoPollThreshold(Float noPollThreshold) {
			this.noPollThreshold = noPollThreshold;
		}

		public Integer getAckCount() {
			return this.ackCount;
		}

		public void setAckCount(Integer ackCount) {
			this.ackCount = ackCount;
		}

		public Duration getAckTime() {
			return this.ackTime;
		}

		public void setAckTime(Duration ackTime) {
			this.ackTime = ackTime;
		}

		public Duration getIdleEventInterval() {
			return this.idleEventInterval;
		}

		public void setIdleEventInterval(Duration idleEventInterval) {
			this.idleEventInterval = idleEventInterval;
		}

		public Duration getMonitorInterval() {
			return this.monitorInterval;
		}

		public void setMonitorInterval(Duration monitorInterval) {
			this.monitorInterval = monitorInterval;
		}

		public Boolean getLogContainerConfig() {
			return this.logContainerConfig;
		}

		public void setLogContainerConfig(Boolean logContainerConfig) {
			this.logContainerConfig = logContainerConfig;
		}

	}

	public static class Ssl {

		/**
		 * Password of the private key in the key store file.
		 */
		private String keyPassword;

		/**
		 * Location of the key store file.
		 */
		private Resource keystoreLocation;

		/**
		 * Store password for the key store file.
		 */
		private String keystorePassword;

		/**
		 * Type of the key store.
		 */
		private String keyStoreType;

		/**
		 * Location of the trust store file.
		 */
		private Resource truststoreLocation;

		/**
		 * Store password for the trust store file.
		 */
		private String truststorePassword;

		/**
		 * Type of the trust store.
		 */
		private String trustStoreType;

		/**
		 * SSL protocol to use.
		 */
		private String protocol;

		public String getKeyPassword() {
			return this.keyPassword;
		}

		public void setKeyPassword(String keyPassword) {
			this.keyPassword = keyPassword;
		}

		public Resource getKeystoreLocation() {
			return this.keystoreLocation;
		}

		public void setKeystoreLocation(Resource keystoreLocation) {
			this.keystoreLocation = keystoreLocation;
		}

		public String getKeystorePassword() {
			return this.keystorePassword;
		}

		public void setKeystorePassword(String keystorePassword) {
			this.keystorePassword = keystorePassword;
		}

		public String getKeyStoreType() {
			return this.keyStoreType;
		}

		public void setKeyStoreType(String keyStoreType) {
			this.keyStoreType = keyStoreType;
		}

		public Resource getTruststoreLocation() {
			return this.truststoreLocation;
		}

		public void setTruststoreLocation(Resource truststoreLocation) {
			this.truststoreLocation = truststoreLocation;
		}

		public String getTruststorePassword() {
			return this.truststorePassword;
		}

		public void setTruststorePassword(String truststorePassword) {
			this.truststorePassword = truststorePassword;
		}

		public String getTrustStoreType() {
			return this.trustStoreType;
		}

		public void setTrustStoreType(String trustStoreType) {
			this.trustStoreType = trustStoreType;
		}

		public String getProtocol() {
			return this.protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public Map<String, Object> buildProperties() {
			Properties properties = new Properties();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getKeyPassword).to(properties.in(SslConfigs.SSL_KEY_PASSWORD_CONFIG));
			map.from(this::getKeystoreLocation).as(this::resourceToPath)
					.to(properties.in(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG));
			map.from(this::getKeystorePassword).to(properties.in(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG));
			map.from(this::getKeyStoreType).to(properties.in(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG));
			map.from(this::getTruststoreLocation).as(this::resourceToPath)
					.to(properties.in(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
			map.from(this::getTruststorePassword).to(properties.in(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
			map.from(this::getTrustStoreType).to(properties.in(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG));
			map.from(this::getProtocol).to(properties.in(SslConfigs.SSL_PROTOCOL_CONFIG));
			return properties;
		}

		private String resourceToPath(Resource resource) {
			try {
				return resource.getFile().getAbsolutePath();
			}
			catch (IOException ex) {
				throw new IllegalStateException("Resource '" + resource + "' must be on a file system", ex);
			}
		}

	}

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

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getLoginModule() {
			return this.loginModule;
		}

		public void setLoginModule(String loginModule) {
			this.loginModule = loginModule;
		}

		public KafkaJaasLoginModuleInitializer.ControlFlag getControlFlag() {
			return this.controlFlag;
		}

		public void setControlFlag(KafkaJaasLoginModuleInitializer.ControlFlag controlFlag) {
			this.controlFlag = controlFlag;
		}

		public Map<String, String> getOptions() {
			return this.options;
		}

		public void setOptions(Map<String, String> options) {
			if (options != null) {
				this.options.putAll(options);
			}
		}

	}

	private static class Properties extends HashMap<String, Object> {

		public <V> java.util.function.Consumer<V> in(String key) {
			return (value) -> put(key, value);
		}

		public Properties with(Ssl ssl, Map<String, String> properties) {
			putAll(ssl.buildProperties());
			putAll(properties);
			return this;
		}

	}

}
