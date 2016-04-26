/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.kafka;

import java.io.IOException;
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
import org.springframework.core.io.Resource;
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;

/**
 * Spring for Apache Kafka Auto-configuration properties.
 *
 * Users should refer to kafka documentation for complete descriptions of these
 * properties.
 *
 * @author Gary Russell
 * @since 1.4
 */
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

	private final Consumer consumer = new Consumer();

	private final Producer producer = new Producer();

	private final Listener listener = new Listener();

	private final Template template = new Template();

	// Apache Kafka Common Properties

	/**
	 * A comma-delimited list of host:port pairs to use for establishing the initial
	 * connection to the Kafka cluster.
	 */
	private List<String> bootstrapServers = Collections.singletonList("localhost:9092");

	/**
	 * An id to pass to the server when making requests; used for server-side logging.
	 */
	private String clientId;

	/**
	 * The password of the private key in the key store file.
	 */
	private String sslKeyPassword;

	/**
	 * The location of the key store file.
	 */
	private Resource sslKeystoreLocation;

	/**
	 * The store password for the key store file.
	 */
	private String sslKeystorePassword;

	/**
	 * The location of the trust store file.
	 */
	private Resource sslTruststoreLocation;

	/**
	 * The store password for the trust store file.
	 */
	private String sslTruststorePassword;

	public Consumer getConsumer() {
		return this.consumer;
	}

	public Producer getProducer() {
		return this.producer;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Template getTemplate() {
		return this.template;
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

	public String getSslKeyPassword() {
		return this.sslKeyPassword;
	}

	public void setSslKeyPassword(String sslKeyPassword) {
		this.sslKeyPassword = sslKeyPassword;
	}

	public Resource getSslKeystoreLocation() {
		return this.sslKeystoreLocation;
	}

	public void setSslKeystoreLocation(Resource sslKeystoreLocation) {
		this.sslKeystoreLocation = sslKeystoreLocation;
	}

	public String getSslKeystorePassword() {
		return this.sslKeystorePassword;
	}

	public void setSslKeystorePassword(String sslKeystorePassword) {
		this.sslKeystorePassword = sslKeystorePassword;
	}

	public Resource getSslTruststoreLocation() {
		return this.sslTruststoreLocation;
	}

	public void setSslTruststoreLocation(Resource sslTruststoreLocation) {
		this.sslTruststoreLocation = sslTruststoreLocation;
	}

	public String getSslTruststorePassword() {
		return this.sslTruststorePassword;
	}

	public void setSslTruststorePassword(String sslTruststorePassword) {
		this.sslTruststorePassword = sslTruststorePassword;
	}

	private Map<String, Object> buildCommonProperties() {
		Map<String, Object> properties = new HashMap<String, Object>();
		if (this.bootstrapServers != null) {
			properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		}
		if (this.clientId != null) {
			properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, this.clientId);
		}
		if (this.sslKeyPassword != null) {
			properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, this.sslKeyPassword);
		}
		if (this.sslKeystoreLocation != null) {
			properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, resourceToPath(this.sslKeystoreLocation));
		}
		if (this.sslKeystorePassword != null) {
			properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, this.sslKeystorePassword);
		}
		if (this.sslTruststoreLocation != null) {
			properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, resourceToPath(this.sslTruststoreLocation));
		}
		if (this.sslTruststorePassword != null) {
			properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, this.sslTruststorePassword);
		}
		return properties;
	}

	public Map<String, Object> buildConsumerProperties() {
		Map<String, Object> props = buildCommonProperties();
		props.putAll(this.consumer.buildProperties());
		return props;
	}

	public Map<String, Object> buildProducerProperties() {
		Map<String, Object> props = buildCommonProperties();
		props.putAll(this.producer.buildProperties());
		return props;
	}

	public static String resourceToPath(Resource resource) {
		try {
			return resource.getFile().getAbsolutePath();
		}
		catch (IOException e) {
			throw new IllegalStateException("Resource must be on a file system", e);
		}
	}

	public static class Consumer {

		/**
		 * The frequency in milliseconds that the consumer offsets are auto-committed to
		 * Kafka if 'enable.auto.commit' true.
		 */
		private Long autoCommitIntervalMs;

		/**
		 * What to do when there is no initial offset in Kafka or if the current offset
		 * does not exist any more on the server.
		 */
		private String autoOffsetReset;

		/**
		 * A comma-delimited list of host:port pairs to use for establishing the initial
		 * connection to the Kafka cluster.
		 */
		private List<String> bootstrapServers;

		/**
		 * An id to pass to the server when making requests; used for server-side logging.
		 */
		private String clientId;

		/**
		 * If true the consumer's offset will be periodically committed in the background.
		 */
		private Boolean enableAutoCommit;

		/**
		 * The maximum amount of time the server will block before answering the fetch
		 * request if there isn't sufficient data to immediately satisfy the requirement
		 * given by fetch.min.bytes.
		 */
		private Integer fetchMaxWaitMs;

		/**
		 * The minimum amount of data the server should return for a fetch request.
		 */
		private Integer fetchMinBytes;

		/**
		 * A unique string that identifies the consumer group this consumer belongs to.
		 */
		private String groupId;

		/**
		 * The expected time between heartbeats to the consumer coordinator.
		 */
		private Integer heartbeatIntervalMs;

		/**
		 * Deserializer class for key that implements the
		 * org.apache.kafka.common.serialization.Deserializer interface.
		 */
		private Class<?> keyDeserializer = StringDeserializer.class;

		/**
		 * The password of the private key in the key store file.
		 */
		private String sslKeyPassword;

		/**
		 * The location of the key store file.
		 */
		private Resource sslKeystoreLocation;

		/**
		 * The store password for the key store file.
		 */
		private String sslKeystorePassword;

		/**
		 * The location of the trust store file.
		 */
		private Resource sslTruststoreLocation;

		/**
		 * The store password for the trust store file.
		 */
		private String sslTruststorePassword;

		/**
		 * Deserializer class for value that implements the
		 * org.apache.kafka.common.serialization.Deserializer interface.
		 */
		private Class<?> valueDeserializer = StringDeserializer.class;

		public Long getAutoCommitIntervalMs() {
			return this.autoCommitIntervalMs;
		}

		public void setAutoCommitIntervalMs(Long autoCommitIntervalMs) {
			this.autoCommitIntervalMs = autoCommitIntervalMs;
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

		public Integer getFetchMaxWaitMs() {
			return this.fetchMaxWaitMs;
		}

		public void setFetchMaxWaitMs(Integer fetchMaxWaitMs) {
			this.fetchMaxWaitMs = fetchMaxWaitMs;
		}

		public Integer getFetchMinBytes() {
			return this.fetchMinBytes;
		}

		public void setFetchMinBytes(Integer fetchMinBytes) {
			this.fetchMinBytes = fetchMinBytes;
		}

		public String getGroupId() {
			return this.groupId;
		}

		public void setGroupId(String groupId) {
			this.groupId = groupId;
		}

		public Integer getHeartbeatIntervalMs() {
			return this.heartbeatIntervalMs;
		}

		public void setHeartbeatIntervalMs(Integer heartbeatIntervalMs) {
			this.heartbeatIntervalMs = heartbeatIntervalMs;
		}

		public Class<?> getKeyDeserializer() {
			return this.keyDeserializer;
		}

		public void setKeyDeserializer(Class<?> keyDeserializer) {
			this.keyDeserializer = keyDeserializer;
		}

		public String getSslKeyPassword() {
			return this.sslKeyPassword;
		}

		public void setSslKeyPassword(String sslKeyPassword) {
			this.sslKeyPassword = sslKeyPassword;
		}

		public Resource getSslKeystoreLocation() {
			return this.sslKeystoreLocation;
		}

		public void setSslKeystoreLocation(Resource sslKeystoreLocation) {
			this.sslKeystoreLocation = sslKeystoreLocation;
		}

		public String getSslKeystorePassword() {
			return this.sslKeystorePassword;
		}

		public void setSslKeystorePassword(String sslKeystorePassword) {
			this.sslKeystorePassword = sslKeystorePassword;
		}

		public Resource getSslTruststoreLocation() {
			return this.sslTruststoreLocation;
		}

		public void setSslTruststoreLocation(Resource sslTruststoreLocation) {
			this.sslTruststoreLocation = sslTruststoreLocation;
		}

		public String getSslTruststorePassword() {
			return this.sslTruststorePassword;
		}

		public void setSslTruststorePassword(String sslTruststorePassword) {
			this.sslTruststorePassword = sslTruststorePassword;
		}

		public Class<?> getValueDeserializer() {
			return this.valueDeserializer;
		}

		public void setValueDeserializer(Class<?> valueDeserializer) {
			this.valueDeserializer = valueDeserializer;
		}

		public Map<String, Object> buildProperties() {
			Map<String, Object> properties = new HashMap<String, Object>();
			if (this.autoCommitIntervalMs != null) {
				properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, this.autoCommitIntervalMs);
			}
			if (this.autoOffsetReset != null) {
				properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, this.autoOffsetReset);
			}
			if (this.bootstrapServers != null) {
				properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
			}
			if (this.clientId != null) {
				properties.put(ConsumerConfig.CLIENT_ID_CONFIG, this.clientId);
			}
			if (this.enableAutoCommit != null) {
				properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, this.enableAutoCommit);
			}
			if (this.fetchMaxWaitMs != null) {
				properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, this.fetchMaxWaitMs);
			}
			if (this.fetchMinBytes != null) {
				properties.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, this.fetchMinBytes);
			}
			if (this.groupId != null) {
				properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
			}
			if (this.heartbeatIntervalMs != null) {
				properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, this.heartbeatIntervalMs);
			}
			if (this.keyDeserializer != null) {
				properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, this.keyDeserializer);
			}
			if (this.sslKeyPassword != null) {
				properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, this.sslKeyPassword);
			}
			if (this.sslKeystoreLocation != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, resourceToPath(this.sslKeystoreLocation));
			}
			if (this.sslKeystorePassword != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, this.sslKeystorePassword);
			}
			if (this.sslTruststoreLocation != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, resourceToPath(this.sslTruststoreLocation));
			}
			if (this.sslTruststorePassword != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, this.sslTruststorePassword);
			}
			if (this.valueDeserializer != null) {
				properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, this.valueDeserializer);
			}
			return properties;
		}

	}

	public static class Producer {

		/**
		 * The number of acknowledgments the producer requires the leader to have
		 * received before considering a request complete.
		 */
		private String acks;

		/**
		 * The number of records to batch before sending.
		 */
		private Integer batchSize;

		/**
		 * A comma-delimited list of host:port pairs to use for establishing the initial
		 * connection to the Kafka cluster.
		 */
		private List<String> bootstrapServers;

		/**
		 * The total bytes of memory the producer can use to buffer records waiting to be
		 * sent to the server.
		 */
		private Long bufferMemory;

		/**
		 * An id to pass to the server when making requests; used for server-side logging.
		 */
		private String clientId;

		/**
		 * The compression type for all data generated by the producer.
		 */
		private String compressionType;

		/**
		 * Serializer class for key that implements the
		 * org.apache.kafka.common.serialization.Serializer interface.
		 */
		private Class<?> keySerializer = StringSerializer.class;

		/**
		 * When greater than zero, enables retrying of failed sends.
		 */
		private Integer retries;

		/**
		 * The password of the private key in the key store file.
		 */
		private String sslKeyPassword;

		/**
		 * The location of the key store file.
		 */
		private Resource sslKeystoreLocation;

		/**
		 * The store password for the key store file.
		 */
		private String sslKeystorePassword;

		/**
		 * The location of the trust store file.
		 */
		private Resource sslTruststoreLocation;

		/**
		 * The store password for the trust store file.
		 */
		private String sslTruststorePassword;

		/**
		 * Serializer class for value that implements the
		 * org.apache.kafka.common.serialization.Serializer interface.
		 */
		private Class<?> valueSerializer = StringSerializer.class;

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

		public Integer getRetries() {
			return this.retries;
		}

		public void setRetries(Integer retries) {
			this.retries = retries;
		}

		public String getSslKeyPassword() {
			return this.sslKeyPassword;
		}

		public void setSslKeyPassword(String sslKeyPassword) {
			this.sslKeyPassword = sslKeyPassword;
		}

		public Resource getSslKeystoreLocation() {
			return this.sslKeystoreLocation;
		}

		public void setSslKeystoreLocation(Resource sslKeystoreLocation) {
			this.sslKeystoreLocation = sslKeystoreLocation;
		}

		public String getSslKeystorePassword() {
			return this.sslKeystorePassword;
		}

		public void setSslKeystorePassword(String sslKeystorePassword) {
			this.sslKeystorePassword = sslKeystorePassword;
		}

		public Resource getSslTruststoreLocation() {
			return this.sslTruststoreLocation;
		}

		public void setSslTruststoreLocation(Resource sslTruststoreLocation) {
			this.sslTruststoreLocation = sslTruststoreLocation;
		}

		public String getSslTruststorePassword() {
			return this.sslTruststorePassword;
		}

		public void setSslTruststorePassword(String sslTruststorePassword) {
			this.sslTruststorePassword = sslTruststorePassword;
		}

		public Class<?> getValueSerializer() {
			return this.valueSerializer;
		}

		public void setValueSerializer(Class<?> valueSerializer) {
			this.valueSerializer = valueSerializer;
		}

		public Map<String, Object> buildProperties() {
			Map<String, Object> properties = new HashMap<String, Object>();
			if (this.acks != null) {
				properties.put(ProducerConfig.ACKS_CONFIG, this.acks);
			}
			if (this.batchSize != null) {
				properties.put(ProducerConfig.BATCH_SIZE_CONFIG, this.batchSize);
			}
			if (this.bootstrapServers != null) {
				properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
			}
			if (this.bufferMemory != null) {
				properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, this.bufferMemory);
			}
			if (this.clientId != null) {
				properties.put(ProducerConfig.CLIENT_ID_CONFIG, this.clientId);
			}
			if (this.compressionType != null) {
				properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, this.compressionType);
			}
			if (this.keySerializer != null) {
				properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, this.keySerializer);
			}
			if (this.retries != null) {
				properties.put(ProducerConfig.RETRIES_CONFIG, this.retries);
			}
			if (this.sslKeyPassword != null) {
				properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, this.sslKeyPassword);
			}
			if (this.sslKeystoreLocation != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, resourceToPath(this.sslKeystoreLocation));
			}
			if (this.sslKeystorePassword != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, this.sslKeystorePassword);
			}
			if (this.sslTruststoreLocation != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, resourceToPath(this.sslTruststoreLocation));
			}
			if (this.sslTruststorePassword != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, this.sslTruststorePassword);
			}
			if (this.valueSerializer != null) {
				properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, this.valueSerializer);
			}
			return properties;
		}

	}

	public static class Template {

		/**
		 * The default topic to which messages will be sent.
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

		/**
		 * The listener AckMode; see the spring-kafka documentation.
		 */
		private AckMode ackMode;

		/**
		 * The number of threads to run in the listener containers.
		 */
		private Integer concurrency;

		/**
		 * The timeout in milliseconds to use when polling the consumer.
		 */
		private Long pollTimeout;

		public AckMode getAckMode() {
			return this.ackMode;
		}

		public void setAckMode(AckMode ackMode) {
			this.ackMode = ackMode;
		}

		public Integer getConcurrency() {
			return this.concurrency;
		}

		public void setConcurrency(Integer concurrency) {
			this.concurrency = concurrency;
		}

		public Long getPollTimeout() {
			return this.pollTimeout;
		}

		public void setPollTimeout(Long pollTimeout) {
			this.pollTimeout = pollTimeout;
		}

	}

}
