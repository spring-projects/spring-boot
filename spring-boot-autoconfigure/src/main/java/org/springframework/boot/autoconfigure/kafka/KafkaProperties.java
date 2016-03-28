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
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;

/**
 * Spring for Apache Kafka Auto-configuration properties.
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

	private List<String> bootstrapServers = Collections.singletonList("localhost:9092");

	private String clientId;

	private String sslKeyPassword;

	private String sslKeystoreLocation;

	private String sslKeystorePassword;

	private String sslTruststoreLocation;

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

	public String getSslKeystoreLocation() {
		return this.sslKeystoreLocation;
	}

	public void setSslKeystoreLocation(String sslKeystoreLocation) {
		this.sslKeystoreLocation = sslKeystoreLocation;
	}

	public String getSslKeystorePassword() {
		return this.sslKeystorePassword;
	}

	public void setSslKeystorePassword(String sslKeystorePassword) {
		this.sslKeystorePassword = sslKeystorePassword;
	}

	public String getSslTruststoreLocation() {
		return this.sslTruststoreLocation;
	}

	public void setSslTruststoreLocation(String sslTruststoreLocation) {
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
			properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, this.sslKeystoreLocation);
		}
		if (this.sslKeystorePassword != null) {
			properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, this.sslKeystorePassword);
		}
		if (this.sslTruststoreLocation != null) {
			properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, this.sslTruststoreLocation);
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

	public static class Consumer {

		private Long autoCommitIntervalMs;

		private String autoOffsetReset;

		private List<String> bootstrapServers;

		private String clientId;

		private Boolean enableAutoCommit;

		private Integer fetchMaxWaitMs;

		private Integer fetchMinBytes;

		private String groupId;

		private Integer heartbeatIntervalMs;

		private Class<?> keyDeserializer = StringDeserializer.class;

		private String sslKeyPassword;

		private String sslKeystoreLocation;

		private String sslKeystorePassword;

		private String sslTruststoreLocation;

		private String sslTruststorePassword;

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

		public String getSslKeystoreLocation() {
			return this.sslKeystoreLocation;
		}

		public void setSslKeystoreLocation(String sslKeystoreLocation) {
			this.sslKeystoreLocation = sslKeystoreLocation;
		}

		public String getSslKeystorePassword() {
			return this.sslKeystorePassword;
		}

		public void setSslKeystorePassword(String sslKeystorePassword) {
			this.sslKeystorePassword = sslKeystorePassword;
		}

		public String getSslTruststoreLocation() {
			return this.sslTruststoreLocation;
		}

		public void setSslTruststoreLocation(String sslTruststoreLocation) {
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
				properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, this.sslKeystoreLocation);
			}
			if (this.sslKeystorePassword != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, this.sslKeystorePassword);
			}
			if (this.sslTruststoreLocation != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, this.sslTruststoreLocation);
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

		private String acks;

		private Integer batchSize;

		private List<String> bootstrapServers;

		private Long bufferMemory;

		private String clientId;

		private String compressionType;

		private Class<?> keySerializer = StringSerializer.class;

		private Integer retries;

		private String sslKeyPassword;

		private String sslKeystoreLocation;

		private String sslKeystorePassword;

		private String sslTruststoreLocation;

		private String sslTruststorePassword;

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

		public String getSslKeystoreLocation() {
			return this.sslKeystoreLocation;
		}

		public void setSslKeystoreLocation(String sslKeystoreLocation) {
			this.sslKeystoreLocation = sslKeystoreLocation;
		}

		public String getSslKeystorePassword() {
			return this.sslKeystorePassword;
		}

		public void setSslKeystorePassword(String sslKeystorePassword) {
			this.sslKeystorePassword = sslKeystorePassword;
		}

		public String getSslTruststoreLocation() {
			return this.sslTruststoreLocation;
		}

		public void setSslTruststoreLocation(String sslTruststoreLocation) {
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
				properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, this.sslKeystoreLocation);
			}
			if (this.sslKeystorePassword != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, this.sslKeystorePassword);
			}
			if (this.sslTruststoreLocation != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, this.sslTruststoreLocation);
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

		private String defaultTopic;

		public String getDefaultTopic() {
			return this.defaultTopic;
		}

		public void setDefaultTopic(String defaultTopic) {
			this.defaultTopic = defaultTopic;
		}

	}

	public static class Listener {

		private AckMode ackMode;

		private Integer concurrency;

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
