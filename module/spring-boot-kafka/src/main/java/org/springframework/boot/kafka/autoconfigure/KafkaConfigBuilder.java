/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.kafka.autoconfigure;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.streams.StreamsConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails.Configuration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Producer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Security;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Ssl;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Streams;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Builder for Kafka components based on {@link KafkaProperties}.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public class KafkaConfigBuilder {

	private static final String PROPERTIES_NAMESPACE = "spring.kafka";

	private final KafkaProperties kafkaProperties;

	protected KafkaConfigBuilder(KafkaProperties kafkaProperties) {
		this.kafkaProperties = kafkaProperties;
	}

	/**
	 * Create a builder using the given {@link KafkaProperties}.
	 * @param kafkaProperties the properties to use
	 * @return a new builder instance
	 */
	public static KafkaConfigBuilder of(KafkaProperties kafkaProperties) {
		return new KafkaConfigBuilder(kafkaProperties);
	}

	/**
	 * Return a builder for Admin-related configuration.
	 * @return an admin config builder
	 * @see AdminClientConfig
	 */
	public ConfigBuilder admin() {
		return admin(this.kafkaProperties.getAdmin());
	}

	/**
	 * Return a builder for Admin-related configuration.
	 * @param adminProperties the admin properties to map
	 * @return an admin config builder
	 * @see AdminClientConfig
	 */
	public ConfigBuilder admin(KafkaProperties.SimpleAdmin adminProperties) {
		return new AdminConfigBuilder(initializeKafkaConfig(), adminProperties, null);
	}

	/**
	 * Return a builder for Consumer-related configuration.
	 * @return a consumer config builder
	 * @see ConsumerConfig
	 */
	public ConfigBuilder consumer() {
		return new ConsumerConfigBuilder(initializeKafkaConfig(), this.kafkaProperties.getConsumer(), null);
	}

	/**
	 * Return a builder for Producer-related configuration.
	 * @return a producer config builder
	 * @see ProducerConfig
	 */
	public ConfigBuilder producer() {
		return new ProducerConfigBuilder(initializeKafkaConfig(), this.kafkaProperties.getProducer(), null);
	}

	/**
	 * Return a builder for Streams-related configuration.
	 * @return a streams config builder
	 * @see StreamsConfig
	 */
	public ConfigBuilder streams() {
		return new StreamsConfigBuilder(initializeKafkaConfig(), this.kafkaProperties.getStreams(), null);
	}

	protected KafkaConfig initializeKafkaConfig() {
		KafkaConfig kafkaConfig = new KafkaConfig();
		kafkaConfig.putIfNonNull(this.kafkaProperties::getBootstrapServers,
				CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
		kafkaConfig.putIfNonNull(this.kafkaProperties::getClientId, CommonClientConfigs.CLIENT_ID_CONFIG);
		new SslConfigBuilder(this.kafkaProperties.getSsl(), PROPERTIES_NAMESPACE + ".ssl").apply(kafkaConfig);
		new SecurityConfigBuilder(this.kafkaProperties.getSecurity()).apply(kafkaConfig);
		kafkaConfig.putAll(this.kafkaProperties.getProperties());
		return kafkaConfig;
	}

	private static void applySecurityProtocol(Map<String, Object> properties, @Nullable String securityProtocol) {
		if (StringUtils.hasLength(securityProtocol)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
		}
	}

	private static void applySslBundle(Map<String, Object> properties, @Nullable SslBundle sslBundle) {
		if (sslBundle != null) {
			properties.put(SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG, SslBundleSslEngineFactory.class);
			properties.put(SslBundle.class.getName(), sslBundle);
		}
	}

	public interface ConfigBuilder {

		/**
		 * Apply the given {@link KafkaConnectionDetails}.
		 * @param connectionDetails the connection details to use
		 * @return {@code this}
		 */
		ConfigBuilder withConnectionDetails(@Nullable KafkaConnectionDetails connectionDetails);

		/**
		 * Build the configuration managed by this builder.
		 * @return the configuration to use
		 */
		Map<String, Object> build();

	}

	protected static class KafkaConfig extends LinkedHashMap<String, Object> {

		public KafkaConfig() {
		}

		public KafkaConfig(Map<? extends String, ?> m) {
			super(m);
		}

		public void putIfNonNull(Supplier<@Nullable Object> value, String key) {
			Object toSet = value.get();
			if (toSet != null) {
				this.put(key, toSet);
			}
		}

		public <V> Consumer<V> in(String key) {
			return (value) -> put(key, value);
		}

	}

	private static final class AdminConfigBuilder implements ConfigBuilder {

		private static final String NAMESPACE = PROPERTIES_NAMESPACE + ".admin";

		private final KafkaConfig kafkaConfig;

		private final KafkaProperties.SimpleAdmin admin;

		private final @Nullable KafkaConnectionDetails connectionDetails;

		private AdminConfigBuilder(KafkaConfig kafkaConfig, KafkaProperties.SimpleAdmin admin,
				@Nullable KafkaConnectionDetails connectionDetails) {
			this.kafkaConfig = kafkaConfig;
			this.admin = admin;
			this.connectionDetails = connectionDetails;
		}

		@Override
		public ConfigBuilder withConnectionDetails(@Nullable KafkaConnectionDetails connectionDetails) {
			return new AdminConfigBuilder(this.kafkaConfig, this.admin, connectionDetails);
		}

		@Override
		public Map<String, Object> build() {
			KafkaConfig adminConfig = new KafkaConfig(this.kafkaConfig);
			PropertyMapper map = PropertyMapper.get();
			map.from(this.admin::getClientId).to(adminConfig.in(ProducerConfig.CLIENT_ID_CONFIG));
			new SslConfigBuilder(this.admin.getSsl(), NAMESPACE + ".ssl").apply(adminConfig);
			new SecurityConfigBuilder(this.admin.getSecurity()).apply(adminConfig);
			adminConfig.putAll(this.admin.getProperties());
			if (this.connectionDetails != null) {
				Configuration admin = this.connectionDetails.getAdmin();
				adminConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, admin.getBootstrapServers());
				applySecurityProtocol(adminConfig, admin.getSecurityProtocol());
				applySslBundle(adminConfig, admin.getSslBundle());
			}
			return adminConfig;
		}

	}

	private static final class ConsumerConfigBuilder implements ConfigBuilder {

		private static final String NAMESPACE = PROPERTIES_NAMESPACE + ".consumer";

		private final KafkaConfig kafkaConfig;

		private final KafkaProperties.Consumer consumer;

		private final @Nullable KafkaConnectionDetails connectionDetails;

		private ConsumerConfigBuilder(KafkaConfig kafkaConfig, KafkaProperties.Consumer consumer,
				@Nullable KafkaConnectionDetails connectionDetails) {
			this.kafkaConfig = kafkaConfig;
			this.consumer = consumer;
			this.connectionDetails = connectionDetails;
		}

		@Override
		public ConsumerConfigBuilder withConnectionDetails(@Nullable KafkaConnectionDetails connectionDetails) {
			return new ConsumerConfigBuilder(this.kafkaConfig, this.consumer, connectionDetails);
		}

		@Override
		public Map<String, Object> build() {
			KafkaConfig consumerConfig = new KafkaConfig(this.kafkaConfig);
			PropertyMapper map = PropertyMapper.get();
			map.from(this.consumer::getAutoCommitInterval)
				.asInt(Duration::toMillis)
				.to(consumerConfig.in(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
			map.from(this.consumer::getAutoOffsetReset).to(consumerConfig.in(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
			map.from(this.consumer::getBootstrapServers).to(consumerConfig.in(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this.consumer::getClientId).to(consumerConfig.in(ConsumerConfig.CLIENT_ID_CONFIG));
			map.from(this.consumer::getEnableAutoCommit)
				.to(consumerConfig.in(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
			map.from(this.consumer::getFetchMaxWait)
				.asInt(Duration::toMillis)
				.to(consumerConfig.in(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG));
			map.from(this.consumer::getFetchMinSize)
				.asInt(DataSize::toBytes)
				.to(consumerConfig.in(ConsumerConfig.FETCH_MIN_BYTES_CONFIG));
			map.from(this.consumer::getGroupId).to(consumerConfig.in(ConsumerConfig.GROUP_ID_CONFIG));
			map.from(this.consumer::getHeartbeatInterval)
				.asInt(Duration::toMillis)
				.to(consumerConfig.in(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
			map.from(() -> this.consumer.getIsolationLevel().name().toLowerCase(Locale.ROOT))
				.to(consumerConfig.in(ConsumerConfig.ISOLATION_LEVEL_CONFIG));
			map.from(this.consumer::getKeyDeserializer)
				.to(consumerConfig.in(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
			map.from(this.consumer::getValueDeserializer)
				.to(consumerConfig.in(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
			map.from(this.consumer::getMaxPollRecords).to(consumerConfig.in(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
			map.from(this.consumer::getMaxPollInterval)
				.asInt(Duration::toMillis)
				.to(consumerConfig.in(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));
			new SslConfigBuilder(this.consumer.getSsl(), NAMESPACE + ".ssl").apply(consumerConfig);
			new SecurityConfigBuilder(this.consumer.getSecurity()).apply(consumerConfig);
			consumerConfig.putAll(this.consumer.getProperties());
			if (this.connectionDetails != null) {
				Configuration consumer = this.connectionDetails.getConsumer();
				consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumer.getBootstrapServers());
				applySecurityProtocol(consumerConfig, consumer.getSecurityProtocol());
				applySslBundle(consumerConfig, consumer.getSslBundle());
			}
			return consumerConfig;
		}

	}

	private static final class ProducerConfigBuilder implements ConfigBuilder {

		private static final String NAMESPACE = PROPERTIES_NAMESPACE + ".producer";

		private final KafkaConfig kafkaConfig;

		private final KafkaProperties.Producer producer;

		private final @Nullable KafkaConnectionDetails connectionDetails;

		private ProducerConfigBuilder(KafkaConfig kafkaConfig, Producer producer,
				@Nullable KafkaConnectionDetails connectionDetails) {
			this.producer = producer;
			this.kafkaConfig = kafkaConfig;
			this.connectionDetails = connectionDetails;
		}

		@Override
		public ProducerConfigBuilder withConnectionDetails(@Nullable KafkaConnectionDetails connectionDetails) {
			return new ProducerConfigBuilder(this.kafkaConfig, this.producer, connectionDetails);
		}

		@Override
		public Map<String, Object> build() {
			KafkaConfig producerConfig = new KafkaConfig(this.kafkaConfig);
			PropertyMapper map = PropertyMapper.get();
			map.from(this.producer::getAcks).to(producerConfig.in(ProducerConfig.ACKS_CONFIG));
			map.from(this.producer::getBatchSize)
				.asInt(DataSize::toBytes)
				.to(producerConfig.in(ProducerConfig.BATCH_SIZE_CONFIG));
			map.from(this.producer::getBootstrapServers).to(producerConfig.in(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this.producer::getBufferMemory)
				.as(DataSize::toBytes)
				.to(producerConfig.in(ProducerConfig.BUFFER_MEMORY_CONFIG));
			map.from(this.producer::getClientId).to(producerConfig.in(ProducerConfig.CLIENT_ID_CONFIG));
			map.from(this.producer::getCompressionType).to(producerConfig.in(ProducerConfig.COMPRESSION_TYPE_CONFIG));
			map.from(this.producer::getKeySerializer).to(producerConfig.in(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
			map.from(this.producer::getRetries).to(producerConfig.in(ProducerConfig.RETRIES_CONFIG));
			map.from(this.producer::getValueSerializer)
				.to(producerConfig.in(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
			new SslConfigBuilder(this.producer.getSsl(), NAMESPACE + ".ssl").apply(producerConfig);
			new SecurityConfigBuilder(this.producer.getSecurity()).apply(producerConfig);
			producerConfig.putAll(this.producer.getProperties());
			if (this.connectionDetails != null) {
				Configuration producer = this.connectionDetails.getProducer();
				producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producer.getBootstrapServers());
				applySecurityProtocol(producerConfig, producer.getSecurityProtocol());
				applySslBundle(producerConfig, producer.getSslBundle());
			}
			return producerConfig;
		}

	}

	private static final class StreamsConfigBuilder implements ConfigBuilder {

		private static final String NAMESPACE = PROPERTIES_NAMESPACE + ".streams";

		private final KafkaConfig kafkaConfig;

		private final Streams streams;

		private final @Nullable KafkaConnectionDetails connectionDetails;

		private StreamsConfigBuilder(KafkaConfig kafkaConfig, Streams streams,
				@Nullable KafkaConnectionDetails connectionDetails) {
			this.kafkaConfig = kafkaConfig;
			this.streams = streams;
			this.connectionDetails = connectionDetails;
		}

		@Override
		public StreamsConfigBuilder withConnectionDetails(@Nullable KafkaConnectionDetails connectionDetails) {
			return new StreamsConfigBuilder(this.kafkaConfig, this.streams, connectionDetails);
		}

		@Override
		public Map<String, Object> build() {
			KafkaConfig streamsConfig = new KafkaConfig(this.kafkaConfig);
			PropertyMapper map = PropertyMapper.get();
			map.from(this.streams::getApplicationId).to(streamsConfig.in("application.id"));
			map.from(this.streams::getBootstrapServers).to(streamsConfig.in(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this.streams::getStateStoreCacheMaxSize)
				.asInt(DataSize::toBytes)
				.to(streamsConfig.in("statestore.cache.max.bytes"));
			map.from(this.streams::getClientId).to(streamsConfig.in(StreamsConfig.CLIENT_ID_CONFIG));
			map.from(this.streams::getReplicationFactor).to(streamsConfig.in("replication.factor"));
			map.from(this.streams::getStateDir).to(streamsConfig.in("state.dir"));
			new SslConfigBuilder(this.streams.getSsl(), NAMESPACE + ".ssl").apply(streamsConfig);
			new SecurityConfigBuilder(this.streams.getSecurity()).apply(streamsConfig);
			streamsConfig.putAll(this.streams.getProperties());
			if (this.connectionDetails != null) {
				Configuration streams = this.connectionDetails.getStreams();
				streamsConfig.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, streams.getBootstrapServers());
				applySecurityProtocol(streamsConfig, streams.getSecurityProtocol());
				applySslBundle(streamsConfig, streams.getSslBundle());
			}
			return streamsConfig;
		}

	}

	private static final class SslConfigBuilder {

		private final Ssl ssl;

		private final String prefix;

		private SslConfigBuilder(Ssl ssl, String prefix) {
			this.ssl = ssl;
			this.prefix = prefix;
		}

		void apply(KafkaConfig kafkaConfig) {
			validate();
			String bundleName = this.ssl.getBundle();
			if (StringUtils.hasText(bundleName)) {
				return;
			}
			PropertyMapper map = PropertyMapper.get();
			map.from(this.ssl::getKeyPassword).to(kafkaConfig.in(SslConfigs.SSL_KEY_PASSWORD_CONFIG));
			map.from(this.ssl::getKeyStoreCertificateChain)
				.to(kafkaConfig.in(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG));
			map.from(this.ssl::getKeyStoreKey).to(kafkaConfig.in(SslConfigs.SSL_KEYSTORE_KEY_CONFIG));
			map.from(this.ssl::getKeyStoreLocation)
				.as(this::resourceToPath)
				.to(kafkaConfig.in(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG));
			map.from(this.ssl::getKeyStorePassword).to(kafkaConfig.in(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG));
			map.from(this.ssl::getKeyStoreType).to(kafkaConfig.in(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG));
			map.from(this.ssl::getTrustStoreCertificates)
				.to(kafkaConfig.in(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG));
			map.from(this.ssl::getTrustStoreLocation)
				.as(this::resourceToPath)
				.to(kafkaConfig.in(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
			map.from(this.ssl::getTrustStorePassword).to(kafkaConfig.in(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
			map.from(this.ssl::getTrustStoreType).to(kafkaConfig.in(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG));
			map.from(this.ssl::getProtocol).to(kafkaConfig.in(SslConfigs.SSL_PROTOCOL_CONFIG));
		}

		private void validate() {
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleMatchingValuesIn((entries) -> {
				entries.put(key("key-store-key"), this.ssl.getKeyStoreKey());
				entries.put(key("key-store-location"), this.ssl.getKeyStoreLocation());
			}, this::hasValue);
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleMatchingValuesIn((entries) -> {
				entries.put(key("trust-store-certificates"), this.ssl.getTrustStoreCertificates());
				entries.put(key("trust-store-location"), this.ssl.getTrustStoreLocation());
			}, this::hasValue);
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleMatchingValuesIn((entries) -> {
				entries.put(key("bundle"), this.ssl.getBundle());
				entries.put(key("key-store-key"), this.ssl.getKeyStoreKey());
			}, this::hasValue);
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleMatchingValuesIn((entries) -> {
				entries.put(key("bundle"), this.ssl.getBundle());
				entries.put(key("key-store-location"), this.ssl.getKeyStoreLocation());
			}, this::hasValue);
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleMatchingValuesIn((entries) -> {
				entries.put(key("bundle"), this.ssl.getBundle());
				entries.put(key("trust-store-certificates"), this.ssl.getTrustStoreCertificates());
			}, this::hasValue);
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleMatchingValuesIn((entries) -> {
				entries.put(key("bundle"), this.ssl.getBundle());
				entries.put(key("trust-store-location"), this.ssl.getTrustStoreLocation());
			}, this::hasValue);
		}

		private String key(String name) {
			return "%s.%s".formatted(this.prefix, name);
		}

		private boolean hasValue(@Nullable Object value) {
			return (value instanceof String string) ? StringUtils.hasText(string) : value != null;
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

	private static final class SecurityConfigBuilder {

		private final Security security;

		SecurityConfigBuilder(Security security) {
			this.security = security;
		}

		void apply(KafkaConfig kafkaConfig) {
			PropertyMapper map = PropertyMapper.get();
			map.from(this.security::getProtocol).to(kafkaConfig.in(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
		}

	}

}
