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

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.IsolationLevel;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Security;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link KafkaConfigBuilder}.
 *
 * @author Stephane Nicoll
 */
class KafkaConfigBuilderTests {

	private static KafkaConnectionDetails connectionDetails(List<String> bootstrapServers,
			@Nullable String securityProtocol, @Nullable SslBundle sslBundle) {
		return new KafkaConnectionDetails() {

			@Override
			public List<String> getBootstrapServers() {
				return bootstrapServers;
			}

			@Override
			public @Nullable String getSecurityProtocol() {
				return securityProtocol;
			}

			@Override
			public @Nullable SslBundle getSslBundle() {
				return sslBundle;
			}

		};
	}

	interface CommonTests {

		Map<String, Object> build(KafkaProperties properties, @Nullable KafkaConnectionDetails connectionDetails);

		Security security(KafkaProperties properties);

		default Map<String, Object> build(KafkaProperties properties) {
			return build(properties, null);
		}

		@Test
		default void baseBootstrapServersIsApplied() {
			KafkaProperties properties = new KafkaProperties();
			properties.setBootstrapServers(List.of("base:9092"));
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, List.of("base:9092"));
		}

		@Test
		default void baseClientIdIsApplied() {
			KafkaProperties properties = new KafkaProperties();
			properties.setClientId("base-client");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(CommonClientConfigs.CLIENT_ID_CONFIG, "base-client");
		}

		@Test
		default void baseSecurityProtocolIsApplied() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSecurity().setProtocol("SASL_PLAINTEXT");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
		}

		@Test
		default void componentSecurityProtocolOverridesBase() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSecurity().setProtocol("PLAINTEXT");
			security(properties).setProtocol("SASL_SSL");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
		}

		@Test
		default void baseAdditionalPropertiesAreApplied() {
			KafkaProperties properties = new KafkaProperties();
			properties.getProperties().put("base.custom", "value");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry("base.custom", "value");
		}

		@Test
		default void connectionDetailsOverridesBootstrapServers() {
			KafkaProperties properties = new KafkaProperties();
			KafkaConnectionDetails connectionDetails = connectionDetails(List.of("details:9092"), null, null);
			Map<String, Object> config = build(properties, connectionDetails);
			assertThat(config).containsEntry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, List.of("details:9092"));
		}

		@Test
		default void connectionDetailsOverridesSecurityProtocol() {
			KafkaProperties properties = new KafkaProperties();
			KafkaConnectionDetails connectionDetails = connectionDetails(List.of("localhost:9092"), "SSL", null);
			Map<String, Object> config = build(properties, connectionDetails);
			assertThat(config).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
		}

		@Test
		default void connectionDetailsWithoutSecurityProtocolDoesNotOverrideExisting() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSecurity().setProtocol("SASL_SSL");
			KafkaConnectionDetails connectionDetails = connectionDetails(List.of("localhost:9092"), null, null);
			Map<String, Object> config = build(properties, connectionDetails);
			assertThat(config).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
		}

		@Test
		default void connectionDetailsAppliesSslBundle() {
			KafkaProperties properties = new KafkaProperties();
			SslBundle sslBundle = mock(SslBundle.class);
			KafkaConnectionDetails connectionDetails = connectionDetails(List.of("localhost:9092"), null, sslBundle);
			Map<String, Object> config = build(properties, connectionDetails);
			assertThat(config).containsEntry(SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG,
					SslBundleSslEngineFactory.class);
			assertThat(config).containsEntry(SslBundle.class.getName(), sslBundle);
		}

		@Test
		default void sslPemConfiguration() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setKeyStoreKey("-----BEGINkey");
			properties.getSsl().setTrustStoreCertificates("-----BEGINtrust");
			properties.getSsl().setKeyStoreCertificateChain("-----BEGINchain");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, "-----BEGINkey");
			assertThat(config).containsEntry(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, "-----BEGINtrust");
			assertThat(config).containsEntry(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, "-----BEGINchain");
		}

		@Test
		default void sslPemConfigurationWithEmptyBundle() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setKeyStoreKey("-----BEGINkey");
			properties.getSsl().setTrustStoreCertificates("-----BEGINtrust");
			properties.getSsl().setKeyStoreCertificateChain("-----BEGINchain");
			properties.getSsl().setBundle("");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, "-----BEGINkey");
			assertThat(config).containsEntry(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, "-----BEGINtrust");
			assertThat(config).containsEntry(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, "-----BEGINchain");
		}

		@Test
		default void sslPropertiesWhenKeyStoreLocationAndKeySetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setKeyStoreKey("-----BEGIN");
			properties.getSsl().setKeyStoreLocation(new ClassPathResource("ksLoc"));
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenTrustStoreLocationAndCertificatesSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setTrustStoreLocation(new ClassPathResource("tsLoc"));
			properties.getSsl().setTrustStoreCertificates("-----BEGIN");
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenKeyStoreLocationAndBundleSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setBundle("myBundle");
			properties.getSsl().setKeyStoreLocation(new ClassPathResource("ksLoc"));
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenKeyStoreKeyAndBundleSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setBundle("myBundle");
			properties.getSsl().setKeyStoreKey("-----BEGIN");
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenTrustStoreLocationAndBundleSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setBundle("myBundle");
			properties.getSsl().setTrustStoreLocation(new ClassPathResource("tsLoc"));
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenTrustStoreCertificatesAndBundleSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setBundle("myBundle");
			properties.getSsl().setTrustStoreCertificates("-----BEGIN");
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

	}

	@Nested
	class AdminConfigTests implements CommonTests {

		@Override
		public Map<String, Object> build(KafkaProperties properties,
				@Nullable KafkaConnectionDetails connectionDetails) {
			return KafkaConfigBuilder.of(properties).admin().withConnectionDetails(connectionDetails).build();
		}

		@Override
		public Security security(KafkaProperties properties) {
			return properties.getAdmin().getSecurity();
		}

		@Test
		void adminPropertiesAreApplied() {
			KafkaProperties properties = new KafkaProperties();
			properties.getAdmin().setClientId("admin-client");
			properties.getAdmin().getProperties().put("admin.custom", "value");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(AdminClientConfig.CLIENT_ID_CONFIG, "admin-client")
				.containsEntry("admin.custom", "value");
		}

	}

	@Nested
	class ConsumerConfigTests implements CommonTests {

		@Override
		public Map<String, Object> build(KafkaProperties properties,
				@Nullable KafkaConnectionDetails connectionDetails) {
			return KafkaConfigBuilder.of(properties).consumer().withConnectionDetails(connectionDetails).build();
		}

		@Override
		public Security security(KafkaProperties properties) {
			return properties.getConsumer().getSecurity();
		}

		@Test
		void consumerPropertiesAreApplied() {
			KafkaProperties properties = new KafkaProperties();
			KafkaProperties.Consumer consumer = properties.getConsumer();
			consumer.setAutoCommitInterval(Duration.ofSeconds(5));
			consumer.setAutoOffsetReset("earliest");
			consumer.setBootstrapServers(List.of("consumer:9092"));
			consumer.setClientId("consumer-client");
			consumer.setEnableAutoCommit(true);
			consumer.setFetchMaxWait(Duration.ofSeconds(2));
			consumer.setFetchMinSize(DataSize.ofKilobytes(2));
			consumer.setGroupId("group");
			consumer.setHeartbeatInterval(Duration.ofSeconds(3));
			consumer.setIsolationLevel(IsolationLevel.READ_COMMITTED);
			consumer.setKeyDeserializer(LongDeserializer.class);
			consumer.setValueDeserializer(LongDeserializer.class);
			consumer.setMaxPollRecords(10);
			consumer.setMaxPollInterval(Duration.ofSeconds(30));
			consumer.getProperties().put("consumer.custom", "value");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000)
				.containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
				.containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("consumer:9092"))
				.containsEntry(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-client")
				.containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
				.containsEntry(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 2000)
				.containsEntry(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, (int) DataSize.ofKilobytes(2).toBytes())
				.containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "group")
				.containsEntry(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000)
				.containsEntry(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
				.containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
				.containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
				.containsEntry(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10)
				.containsEntry(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 30000)
				.containsEntry("consumer.custom", "value");
		}

	}

	@Nested
	class ProducerConfigTests implements CommonTests {

		@Override
		public Map<String, Object> build(KafkaProperties properties,
				@Nullable KafkaConnectionDetails connectionDetails) {
			return KafkaConfigBuilder.of(properties).producer().withConnectionDetails(connectionDetails).build();
		}

		@Override
		public Security security(KafkaProperties properties) {
			return properties.getProducer().getSecurity();
		}

		@Test
		void producerPropertiesAreApplied() {
			KafkaProperties properties = new KafkaProperties();
			KafkaProperties.Producer producer = properties.getProducer();
			producer.setAcks("all");
			producer.setBatchSize(DataSize.ofKilobytes(16));
			producer.setBootstrapServers(List.of("producer:9092"));
			producer.setBufferMemory(DataSize.ofMegabytes(32));
			producer.setClientId("producer-client");
			producer.setCompressionType("gzip");
			producer.setKeySerializer(LongSerializer.class);
			producer.setRetries(3);
			producer.setValueSerializer(LongSerializer.class);
			producer.getProperties().put("producer.custom", "value");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(ProducerConfig.ACKS_CONFIG, "all")
				.containsEntry(ProducerConfig.BATCH_SIZE_CONFIG, (int) DataSize.ofKilobytes(16).toBytes())
				.containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("producer:9092"))
				.containsEntry(ProducerConfig.BUFFER_MEMORY_CONFIG, DataSize.ofMegabytes(32).toBytes())
				.containsEntry(ProducerConfig.CLIENT_ID_CONFIG, "producer-client")
				.containsEntry(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip")
				.containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class)
				.containsEntry(ProducerConfig.RETRIES_CONFIG, 3)
				.containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, LongSerializer.class)
				.containsEntry("producer.custom", "value");
		}

	}

	@Nested
	class StreamsConfigTests implements CommonTests {

		@Override
		public Map<String, Object> build(KafkaProperties properties,
				@Nullable KafkaConnectionDetails connectionDetails) {
			return KafkaConfigBuilder.of(properties).streams().withConnectionDetails(connectionDetails).build();
		}

		@Override
		public Security security(KafkaProperties properties) {
			return properties.getStreams().getSecurity();
		}

		@Test
		void streamsPropertiesAreApplied() {
			KafkaProperties properties = new KafkaProperties();
			KafkaProperties.Streams streams = properties.getStreams();
			streams.setApplicationId("app-id");
			streams.setBootstrapServers(List.of("streams:9092"));
			streams.setStateStoreCacheMaxSize(DataSize.ofMegabytes(10));
			streams.setClientId("streams-client");
			streams.setReplicationFactor(3);
			streams.setStateDir("/tmp/state");
			streams.getProperties().put("streams.custom", "value");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry("application.id", "app-id")
				.containsEntry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("streams:9092"))
				.containsEntry("statestore.cache.max.bytes", (int) DataSize.ofMegabytes(10).toBytes())
				.containsEntry(StreamsConfig.CLIENT_ID_CONFIG, "streams-client")
				.containsEntry("replication.factor", 3)
				.containsEntry("state.dir", "/tmp/state")
				.containsEntry("streams.custom", "value");
		}

	}

}
