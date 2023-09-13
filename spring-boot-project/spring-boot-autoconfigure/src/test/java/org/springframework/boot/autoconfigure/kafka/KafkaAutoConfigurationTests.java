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

package org.springframework.boot.autoconfigure.kafka;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.assertj.SimpleAsyncTaskExecutorAssert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.CleanupConfig;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.retrytopic.DestinationTopic;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link KafkaAutoConfiguration}.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Nakul Mishra
 * @author Tomaz Fernandes
 * @author Thomas Kåsene
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class KafkaAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class));

	@Test
	void consumerProperties() {
		this.contextRunner.withPropertyValues("spring.kafka.bootstrap-servers=foo:1234",
				"spring.kafka.properties.foo=bar", "spring.kafka.properties.baz=qux",
				"spring.kafka.properties.foo.bar.baz=qux.fiz.buz", "spring.kafka.ssl.key-password=p1",
				"spring.kafka.ssl.key-store-location=classpath:ksLoc", "spring.kafka.ssl.key-store-password=p2",
				"spring.kafka.ssl.key-store-type=PKCS12", "spring.kafka.ssl.trust-store-location=classpath:tsLoc",
				"spring.kafka.ssl.trust-store-password=p3", "spring.kafka.ssl.trust-store-type=PKCS12",
				"spring.kafka.ssl.protocol=TLSv1.2", "spring.kafka.consumer.auto-commit-interval=123",
				"spring.kafka.consumer.max-poll-records=42", "spring.kafka.consumer.auto-offset-reset=earliest",
				"spring.kafka.consumer.client-id=ccid", // test override common
				"spring.kafka.consumer.enable-auto-commit=false", "spring.kafka.consumer.fetch-max-wait=456",
				"spring.kafka.consumer.properties.fiz.buz=fix.fox", "spring.kafka.consumer.fetch-min-size=1KB",
				"spring.kafka.consumer.group-id=bar", "spring.kafka.consumer.heartbeat-interval=234",
				"spring.kafka.consumer.isolation-level = read-committed",
				"spring.kafka.consumer.security.protocol = SSL",
				"spring.kafka.consumer.key-deserializer = org.apache.kafka.common.serialization.LongDeserializer",
				"spring.kafka.consumer.value-deserializer = org.apache.kafka.common.serialization.IntegerDeserializer")
			.run((context) -> {
				DefaultKafkaConsumerFactory<?, ?> consumerFactory = context.getBean(DefaultKafkaConsumerFactory.class);
				Map<String, Object> configs = consumerFactory.getConfigurationProperties();
				// common
				assertThat(configs).containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("foo:1234"));
				assertThat(configs).containsEntry(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "p1");
				assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
					.endsWith(File.separator + "ksLoc");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "p2");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
				assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
					.endsWith(File.separator + "tsLoc");
				assertThat(configs).containsEntry(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "p3");
				assertThat(configs).containsEntry(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
				assertThat(configs).containsEntry(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
				// consumer
				assertThat(configs).containsEntry(ConsumerConfig.CLIENT_ID_CONFIG, "ccid"); // override
				assertThat(configs).containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.FALSE);
				assertThat(configs).containsEntry(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 123);
				assertThat(configs).containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
				assertThat(configs).containsEntry(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 456);
				assertThat(configs).containsEntry(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
				assertThat(configs).containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "bar");
				assertThat(configs).containsEntry(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 234);
				assertThat(configs).containsEntry(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
				assertThat(configs).containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
				assertThat(configs).containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
						IntegerDeserializer.class);
				assertThat(configs).containsEntry(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 42);
				assertThat(configs).containsEntry("foo", "bar");
				assertThat(configs).containsEntry("baz", "qux");
				assertThat(configs).containsEntry("foo.bar.baz", "qux.fiz.buz");
				assertThat(configs).containsEntry("fiz.buz", "fix.fox");
			});
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(PropertiesKafkaConnectionDetails.class));
	}

	@Test
	void connectionDetailsAreAppliedToConsumer() {
		this.contextRunner
			.withPropertyValues("spring.kafka.bootstrap-servers=foo:1234",
					"spring.kafka.consumer.bootstrap-servers=foo:1234", "spring.kafka.security.protocol=SSL",
					"spring.kafka.consumer.security.protocol=SSL")
			.withBean(KafkaConnectionDetails.class, this::kafkaConnectionDetails)
			.run((context) -> {
				assertThat(context).hasSingleBean(KafkaConnectionDetails.class)
					.doesNotHaveBean(PropertiesKafkaConnectionDetails.class);
				DefaultKafkaConsumerFactory<?, ?> consumerFactory = context.getBean(DefaultKafkaConsumerFactory.class);
				Map<String, Object> configs = consumerFactory.getConfigurationProperties();
				assertThat(configs).containsEntry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("kafka.example.com:12345"));
				assertThat(configs).containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("kafka.example.com:12345"));
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
			});
	}

	@Test
	void producerProperties() {
		this.contextRunner.withPropertyValues("spring.kafka.clientId=cid",
				"spring.kafka.properties.foo.bar.baz=qux.fiz.buz", "spring.kafka.producer.acks=all",
				"spring.kafka.producer.batch-size=2KB", "spring.kafka.producer.bootstrap-servers=bar:1234", // test
				// override
				"spring.kafka.producer.buffer-memory=4KB", "spring.kafka.producer.compression-type=gzip",
				"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.LongSerializer",
				"spring.kafka.producer.retries=2", "spring.kafka.producer.properties.fiz.buz=fix.fox",
				"spring.kafka.producer.security.protocol=SSL", "spring.kafka.producer.ssl.key-password=p4",
				"spring.kafka.producer.ssl.key-store-location=classpath:ksLocP",
				"spring.kafka.producer.ssl.key-store-password=p5", "spring.kafka.producer.ssl.key-store-type=PKCS12",
				"spring.kafka.producer.ssl.trust-store-location=classpath:tsLocP",
				"spring.kafka.producer.ssl.trust-store-password=p6",
				"spring.kafka.producer.ssl.trust-store-type=PKCS12", "spring.kafka.producer.ssl.protocol=TLSv1.2",
				"spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.IntegerSerializer")
			.run((context) -> {
				DefaultKafkaProducerFactory<?, ?> producerFactory = context.getBean(DefaultKafkaProducerFactory.class);
				Map<String, Object> configs = producerFactory.getConfigurationProperties();
				// common
				assertThat(configs).containsEntry(ProducerConfig.CLIENT_ID_CONFIG, "cid");
				// producer
				assertThat(configs).containsEntry(ProducerConfig.ACKS_CONFIG, "all");
				assertThat(configs).containsEntry(ProducerConfig.BATCH_SIZE_CONFIG, 2048);
				assertThat(configs).containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("bar:1234")); // override
				assertThat(configs).containsEntry(ProducerConfig.BUFFER_MEMORY_CONFIG, 4096L);
				assertThat(configs).containsEntry(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
				assertThat(configs).containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "p4");
				assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
					.endsWith(File.separator + "ksLocP");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "p5");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
				assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
					.endsWith(File.separator + "tsLocP");
				assertThat(configs).containsEntry(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "p6");
				assertThat(configs).containsEntry(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
				assertThat(configs).containsEntry(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
				assertThat(configs).containsEntry(ProducerConfig.RETRIES_CONFIG, 2);
				assertThat(configs).containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
						IntegerSerializer.class);
				assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).isEmpty();
				assertThat(context.getBeansOfType(KafkaTransactionManager.class)).isEmpty();
				assertThat(configs).containsEntry("foo.bar.baz", "qux.fiz.buz");
				assertThat(configs).containsEntry("fiz.buz", "fix.fox");
			});
	}

	@Test
	void connectionDetailsAreAppliedToProducer() {
		this.contextRunner
			.withPropertyValues("spring.kafka.bootstrap-servers=foo:1234",
					"spring.kafka.producer.bootstrap-servers=foo:1234", "spring.kafka.security.protocol=SSL",
					"spring.kafka.producer.security.protocol=SSL")
			.withBean(KafkaConnectionDetails.class, this::kafkaConnectionDetails)
			.run((context) -> {
				assertThat(context).hasSingleBean(KafkaConnectionDetails.class)
					.doesNotHaveBean(PropertiesKafkaConnectionDetails.class);
				DefaultKafkaProducerFactory<?, ?> producerFactory = context.getBean(DefaultKafkaProducerFactory.class);
				Map<String, Object> configs = producerFactory.getConfigurationProperties();
				assertThat(configs).containsEntry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("kafka.example.com:12345"));
				assertThat(configs).containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("kafka.example.com:12345"));
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
			});
	}

	@Test
	void adminProperties() {
		this.contextRunner
			.withPropertyValues("spring.kafka.clientId=cid", "spring.kafka.properties.foo.bar.baz=qux.fiz.buz",
					"spring.kafka.admin.fail-fast=true", "spring.kafka.admin.properties.fiz.buz=fix.fox",
					"spring.kafka.admin.security.protocol=SSL", "spring.kafka.admin.ssl.key-password=p4",
					"spring.kafka.admin.ssl.key-store-location=classpath:ksLocP",
					"spring.kafka.admin.ssl.key-store-password=p5", "spring.kafka.admin.ssl.key-store-type=PKCS12",
					"spring.kafka.admin.ssl.trust-store-location=classpath:tsLocP",
					"spring.kafka.admin.ssl.trust-store-password=p6", "spring.kafka.admin.ssl.trust-store-type=PKCS12",
					"spring.kafka.admin.ssl.protocol=TLSv1.2", "spring.kafka.admin.close-timeout=35s",
					"spring.kafka.admin.operation-timeout=60s", "spring.kafka.admin.modify-topic-configs=true",
					"spring.kafka.admin.auto-create=false")
			.run((context) -> {
				KafkaAdmin admin = context.getBean(KafkaAdmin.class);
				Map<String, Object> configs = admin.getConfigurationProperties();
				// common
				assertThat(configs).containsEntry(AdminClientConfig.CLIENT_ID_CONFIG, "cid");
				// admin
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "p4");
				assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
					.endsWith(File.separator + "ksLocP");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "p5");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
				assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
					.endsWith(File.separator + "tsLocP");
				assertThat(configs).containsEntry(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "p6");
				assertThat(configs).containsEntry(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
				assertThat(configs).containsEntry(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
				assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).isEmpty();
				assertThat(configs).containsEntry("foo.bar.baz", "qux.fiz.buz");
				assertThat(configs).containsEntry("fiz.buz", "fix.fox");
				assertThat(admin).hasFieldOrPropertyWithValue("closeTimeout", Duration.ofSeconds(35));
				assertThat(admin).hasFieldOrPropertyWithValue("operationTimeout", 60);
				assertThat(admin).hasFieldOrPropertyWithValue("fatalIfBrokerNotAvailable", true);
				assertThat(admin).hasFieldOrPropertyWithValue("modifyTopicConfigs", true);
				assertThat(admin).hasFieldOrPropertyWithValue("autoCreate", false);
			});
	}

	@Test
	void connectionDetailsAreAppliedToAdmin() {
		this.contextRunner
			.withPropertyValues("spring.kafka.bootstrap-servers=foo:1234", "spring.kafka.security.protocol=SSL",
					"spring.kafka.admin.security.protocol=SSL")
			.withBean(KafkaConnectionDetails.class, this::kafkaConnectionDetails)
			.run((context) -> {
				assertThat(context).hasSingleBean(KafkaConnectionDetails.class)
					.doesNotHaveBean(PropertiesKafkaConnectionDetails.class);
				KafkaAdmin admin = context.getBean(KafkaAdmin.class);
				Map<String, Object> configs = admin.getConfigurationProperties();
				assertThat(configs).containsEntry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("kafka.example.com:12345"));
				assertThat(configs).containsEntry(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("kafka.example.com:12345"));
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
				assertThat(configs).containsEntry(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
			});
	}

	@SuppressWarnings("unchecked")
	@Test
	void streamsProperties() {
		this.contextRunner.withUserConfiguration(EnableKafkaStreamsConfiguration.class)
			.withPropertyValues("spring.kafka.client-id=cid",
					"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093", "spring.application.name=appName",
					"spring.kafka.properties.foo.bar.baz=qux.fiz.buz", "spring.kafka.streams.auto-startup=false",
					"spring.kafka.streams.state-store-cache-max-size=1KB", "spring.kafka.streams.client-id=override",
					"spring.kafka.streams.properties.fiz.buz=fix.fox", "spring.kafka.streams.replication-factor=2",
					"spring.kafka.streams.state-dir=/tmp/state", "spring.kafka.streams.security.protocol=SSL",
					"spring.kafka.streams.ssl.key-password=p7",
					"spring.kafka.streams.ssl.key-store-location=classpath:ksLocP",
					"spring.kafka.streams.ssl.key-store-password=p8", "spring.kafka.streams.ssl.key-store-type=PKCS12",
					"spring.kafka.streams.ssl.trust-store-location=classpath:tsLocP",
					"spring.kafka.streams.ssl.trust-store-password=p9",
					"spring.kafka.streams.ssl.trust-store-type=PKCS12", "spring.kafka.streams.ssl.protocol=TLSv1.2")
			.run((context) -> {
				Properties configs = context
					.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME,
							KafkaStreamsConfiguration.class)
					.asProperties();
				assertThat((List<String>) configs.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
					.containsExactly("localhost:9092", "localhost:9093");
				assertThat(configs).containsEntry(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 1024);
				assertThat(configs).containsEntry(StreamsConfig.CLIENT_ID_CONFIG, "override");
				assertThat(configs).containsEntry(StreamsConfig.REPLICATION_FACTOR_CONFIG, 2);
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
				assertThat(configs).containsEntry(StreamsConfig.STATE_DIR_CONFIG, "/tmp/state");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "p7");
				assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
					.endsWith(File.separator + "ksLocP");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "p8");
				assertThat(configs).containsEntry(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
				assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
					.endsWith(File.separator + "tsLocP");
				assertThat(configs).containsEntry(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "p9");
				assertThat(configs).containsEntry(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
				assertThat(configs).containsEntry(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
				assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).isEmpty();
				assertThat(configs).containsEntry("foo.bar.baz", "qux.fiz.buz");
				assertThat(configs).containsEntry("fiz.buz", "fix.fox");
				assertThat(context.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME))
					.isNotNull();
			});
	}

	@Test
	void connectionDetailsAreAppliedToStreams() {
		this.contextRunner.withUserConfiguration(EnableKafkaStreamsConfiguration.class)
			.withPropertyValues("spring.kafka.streams.auto-startup=false", "spring.kafka.streams.application-id=test",
					"spring.kafka.bootstrap-servers=foo:1234", "spring.kafka.streams.bootstrap-servers=foo:1234",
					"spring.kafka.security.protocol=SSL", "spring.kafka.streams.security.protocol=SSL")
			.withBean(KafkaConnectionDetails.class, this::kafkaConnectionDetails)
			.run((context) -> {
				assertThat(context).hasSingleBean(KafkaConnectionDetails.class)
					.doesNotHaveBean(PropertiesKafkaConnectionDetails.class);
				Properties configs = context
					.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME,
							KafkaStreamsConfiguration.class)
					.asProperties();
				assertThat(configs).containsEntry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("kafka.example.com:12345"));
				assertThat(configs).containsEntry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
						Collections.singletonList("kafka.example.com:12345"));
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
				assertThat(configs).containsEntry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
			});
	}

	@SuppressWarnings("deprecation")
	@Deprecated(since = "3.1.0", forRemoval = true)
	void streamsCacheMaxSizeBuffering() {
		this.contextRunner.withUserConfiguration(EnableKafkaStreamsConfiguration.class)
			.withPropertyValues("spring.kafka.streams.cache-max-size-buffering=1KB")
			.run((context) -> {
				Properties configs = context
					.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME,
							KafkaStreamsConfiguration.class)
					.asProperties();
				assertThat(configs).containsEntry(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 1024);
			});
	}

	@SuppressWarnings("unchecked")
	@Test
	void streamsApplicationIdUsesMainApplicationNameByDefault() {
		this.contextRunner.withUserConfiguration(EnableKafkaStreamsConfiguration.class)
			.withPropertyValues("spring.application.name=my-test-app",
					"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093",
					"spring.kafka.streams.auto-startup=false")
			.run((context) -> {
				Properties configs = context
					.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME,
							KafkaStreamsConfiguration.class)
					.asProperties();
				assertThat((List<String>) configs.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
					.containsExactly("localhost:9092", "localhost:9093");
				assertThat(configs).containsEntry(StreamsConfig.APPLICATION_ID_CONFIG, "my-test-app");
			});
	}

	@Test
	void streamsWithCustomKafkaConfiguration() {
		this.contextRunner
			.withUserConfiguration(EnableKafkaStreamsConfiguration.class, TestKafkaStreamsConfiguration.class)
			.withPropertyValues("spring.application.name=my-test-app",
					"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093",
					"spring.kafka.streams.auto-startup=false")
			.run((context) -> {
				Properties configs = context
					.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME,
							KafkaStreamsConfiguration.class)
					.asProperties();
				assertThat(configs).containsEntry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
						"localhost:9094, localhost:9095");
				assertThat(configs).containsEntry(StreamsConfig.APPLICATION_ID_CONFIG, "test-id");
			});
	}

	@Test
	void retryTopicConfigurationIsNotEnabledByDefault() {
		this.contextRunner
			.withPropertyValues("spring.application.name=my-test-app",
					"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093")
			.run((context) -> assertThat(context).doesNotHaveBean(RetryTopicConfiguration.class));
	}

	@Test
	void retryTopicConfigurationWithExponentialBackOff() {
		this.contextRunner.withPropertyValues("spring.application.name=my-test-app",
				"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093", "spring.kafka.retry.topic.enabled=true",
				"spring.kafka.retry.topic.attempts=5", "spring.kafka.retry.topic.delay=100ms",
				"spring.kafka.retry.topic.multiplier=2", "spring.kafka.retry.topic.max-delay=300ms")
			.run((context) -> {
				RetryTopicConfiguration configuration = context.getBean(RetryTopicConfiguration.class);
				assertThat(configuration.getDestinationTopicProperties()).hasSize(5)
					.extracting(DestinationTopic.Properties::delay, DestinationTopic.Properties::suffix)
					.containsExactly(tuple(0L, ""), tuple(100L, "-retry-0"), tuple(200L, "-retry-1"),
							tuple(300L, "-retry-2"), tuple(0L, "-dlt"));
			});
	}

	@Test
	void retryTopicConfigurationWithDefaultProperties() {
		this.contextRunner.withPropertyValues("spring.application.name=my-test-app",
				"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093", "spring.kafka.retry.topic.enabled=true")
			.run(assertRetryTopicConfiguration((configuration) -> {
				assertThat(configuration.getDestinationTopicProperties()).hasSize(3)
					.extracting(DestinationTopic.Properties::delay, DestinationTopic.Properties::suffix)
					.containsExactly(tuple(0L, ""), tuple(1000L, "-retry"), tuple(0L, "-dlt"));
				assertThat(configuration.forKafkaTopicAutoCreation()).extracting("shouldCreateTopics")
					.asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
					.isFalse();
			}));
	}

	@Test
	void retryTopicConfigurationWithFixedBackOff() {
		this.contextRunner.withPropertyValues("spring.application.name=my-test-app",
				"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093", "spring.kafka.retry.topic.enabled=true",
				"spring.kafka.retry.topic.attempts=4", "spring.kafka.retry.topic.delay=2s")
			.run(assertRetryTopicConfiguration(
					(configuration) -> assertThat(configuration.getDestinationTopicProperties()).hasSize(3)
						.extracting(DestinationTopic.Properties::delay)
						.containsExactly(0L, 2000L, 0L)));
	}

	@Test
	void retryTopicConfigurationWithNoBackOff() {
		this.contextRunner.withPropertyValues("spring.application.name=my-test-app",
				"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093", "spring.kafka.retry.topic.enabled=true",
				"spring.kafka.retry.topic.attempts=4", "spring.kafka.retry.topic.delay=0")
			.run(assertRetryTopicConfiguration(
					(configuration) -> assertThat(configuration.getDestinationTopicProperties()).hasSize(3)
						.extracting(DestinationTopic.Properties::delay)
						.containsExactly(0L, 0L, 0L)));
	}

	private ContextConsumer<AssertableApplicationContext> assertRetryTopicConfiguration(
			Consumer<RetryTopicConfiguration> configuration) {
		return (context) -> {
			assertThat(context).hasSingleBean(RetryTopicConfiguration.class);
			configuration.accept(context.getBean(RetryTopicConfiguration.class));
		};
	}

	@SuppressWarnings("unchecked")
	@Test
	void streamsWithSeveralStreamsBuilderFactoryBeans() {
		this.contextRunner
			.withUserConfiguration(EnableKafkaStreamsConfiguration.class,
					TestStreamsBuilderFactoryBeanConfiguration.class)
			.withPropertyValues("spring.application.name=my-test-app",
					"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093",
					"spring.kafka.streams.auto-startup=false")
			.run((context) -> {
				Properties configs = context
					.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME,
							KafkaStreamsConfiguration.class)
					.asProperties();
				assertThat((List<String>) configs.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
					.containsExactly("localhost:9092", "localhost:9093");
				then(context.getBean("&firstStreamsBuilderFactoryBean", StreamsBuilderFactoryBean.class))
					.should(never())
					.setAutoStartup(false);
				then(context.getBean("&secondStreamsBuilderFactoryBean", StreamsBuilderFactoryBean.class))
					.should(never())
					.setAutoStartup(false);
			});
	}

	@Test
	void streamsWithCleanupConfig() {
		this.contextRunner
			.withUserConfiguration(EnableKafkaStreamsConfiguration.class, TestKafkaStreamsConfiguration.class)
			.withPropertyValues("spring.application.name=my-test-app",
					"spring.kafka.bootstrap-servers=localhost:9092,localhost:9093",
					"spring.kafka.streams.auto-startup=false", "spring.kafka.streams.cleanup.on-startup=true",
					"spring.kafka.streams.cleanup.on-shutdown=false")
			.run((context) -> {
				StreamsBuilderFactoryBean streamsBuilderFactoryBean = context.getBean(StreamsBuilderFactoryBean.class);
				assertThat(streamsBuilderFactoryBean)
					.extracting("cleanupConfig", InstanceOfAssertFactories.type(CleanupConfig.class))
					.satisfies((cleanupConfig) -> {
						assertThat(cleanupConfig.cleanupOnStart()).isTrue();
						assertThat(cleanupConfig.cleanupOnStop()).isFalse();
					});
			});
	}

	@Test
	void streamsApplicationIdIsMandatory() {
		this.contextRunner.withUserConfiguration(EnableKafkaStreamsConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure()
				.hasMessageContaining("spring.kafka.streams.application-id")
				.hasMessageContaining(
						"This property is mandatory and fallback 'spring.application.name' is not set either.");

		});
	}

	@Test
	void streamsApplicationIdIsNotMandatoryIfEnableKafkaStreamsIsNotSet() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(StreamsBuilder.class);
		});
	}

	@Test
	void shouldUsePlatformThreadsByDefault() {
		this.contextRunner.run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).isNotNull();
			AsyncTaskExecutor listenerTaskExecutor = factory.getContainerProperties().getListenerTaskExecutor();
			assertThat(listenerTaskExecutor).isNull();
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldUseVirtualThreadsIfEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true").run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).isNotNull();
			AsyncTaskExecutor listenerTaskExecutor = factory.getContainerProperties().getListenerTaskExecutor();
			assertThat(listenerTaskExecutor).isInstanceOf(SimpleAsyncTaskExecutor.class);
			SimpleAsyncTaskExecutorAssert.assertThat((SimpleAsyncTaskExecutor) listenerTaskExecutor)
				.usesVirtualThreads();
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	void listenerProperties() {
		this.contextRunner
			.withPropertyValues("spring.kafka.template.default-topic=testTopic",
					"spring.kafka.template.transaction-id-prefix=txOverride", "spring.kafka.listener.ack-mode=MANUAL",
					"spring.kafka.listener.client-id=client", "spring.kafka.listener.ack-count=123",
					"spring.kafka.listener.ack-time=456", "spring.kafka.listener.concurrency=3",
					"spring.kafka.listener.poll-timeout=2000", "spring.kafka.listener.no-poll-threshold=2.5",
					"spring.kafka.listener.type=batch", "spring.kafka.listener.idle-between-polls=1s",
					"spring.kafka.listener.idle-event-interval=1s",
					"spring.kafka.listener.idle-partition-event-interval=1s",
					"spring.kafka.listener.monitor-interval=45", "spring.kafka.listener.log-container-config=true",
					"spring.kafka.listener.missing-topics-fatal=true", "spring.kafka.jaas.enabled=true",
					"spring.kafka.listener.immediate-stop=true", "spring.kafka.producer.transaction-id-prefix=foo",
					"spring.kafka.jaas.login-module=foo", "spring.kafka.jaas.control-flag=REQUISITE",
					"spring.kafka.jaas.options.useKeyTab=true", "spring.kafka.listener.async-acks=true")
			.run((context) -> {
				DefaultKafkaProducerFactory<?, ?> producerFactory = context.getBean(DefaultKafkaProducerFactory.class);
				DefaultKafkaConsumerFactory<?, ?> consumerFactory = context.getBean(DefaultKafkaConsumerFactory.class);
				KafkaTemplate<?, ?> kafkaTemplate = context.getBean(KafkaTemplate.class);
				AbstractKafkaListenerContainerFactory<?, ?, ?> kafkaListenerContainerFactory = (AbstractKafkaListenerContainerFactory<?, ?, ?>) context
					.getBean(KafkaListenerContainerFactory.class);
				assertThat(kafkaTemplate.getMessageConverter()).isInstanceOf(MessagingMessageConverter.class);
				assertThat(kafkaTemplate).hasFieldOrPropertyWithValue("producerFactory", producerFactory);
				assertThat(kafkaTemplate.getDefaultTopic()).isEqualTo("testTopic");
				assertThat(kafkaTemplate).hasFieldOrPropertyWithValue("transactionIdPrefix", "txOverride");
				assertThat(kafkaListenerContainerFactory.getConsumerFactory()).isEqualTo(consumerFactory);
				ContainerProperties containerProperties = kafkaListenerContainerFactory.getContainerProperties();
				assertThat(containerProperties.getAckMode()).isEqualTo(AckMode.MANUAL);
				assertThat(containerProperties.isAsyncAcks()).isTrue();
				assertThat(containerProperties.getClientId()).isEqualTo("client");
				assertThat(containerProperties.getAckCount()).isEqualTo(123);
				assertThat(containerProperties.getAckTime()).isEqualTo(456L);
				assertThat(containerProperties.getPollTimeout()).isEqualTo(2000L);
				assertThat(containerProperties.getNoPollThreshold()).isEqualTo(2.5f);
				assertThat(containerProperties.getIdleBetweenPolls()).isEqualTo(1000L);
				assertThat(containerProperties.getIdleEventInterval()).isEqualTo(1000L);
				assertThat(containerProperties.getIdlePartitionEventInterval()).isEqualTo(1000L);
				assertThat(containerProperties.getMonitorInterval()).isEqualTo(45);
				assertThat(containerProperties.isLogContainerConfig()).isTrue();
				assertThat(containerProperties.isMissingTopicsFatal()).isTrue();
				assertThat(containerProperties.isStopImmediate()).isTrue();
				assertThat(kafkaListenerContainerFactory).extracting("concurrency").isEqualTo(3);
				assertThat(kafkaListenerContainerFactory.isBatchListener()).isTrue();
				assertThat(kafkaListenerContainerFactory).hasFieldOrPropertyWithValue("autoStartup", true);
				assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).hasSize(1);
				KafkaJaasLoginModuleInitializer jaas = context.getBean(KafkaJaasLoginModuleInitializer.class);
				assertThat(jaas).hasFieldOrPropertyWithValue("loginModule", "foo");
				assertThat(jaas).hasFieldOrPropertyWithValue("controlFlag",
						AppConfigurationEntry.LoginModuleControlFlag.REQUISITE);
				assertThat(context.getBeansOfType(KafkaTransactionManager.class)).hasSize(1);
				assertThat(((Map<String, String>) ReflectionTestUtils.getField(jaas, "options")))
					.containsExactly(entry("useKeyTab", "true"));
			});
	}

	@Test
	void testKafkaTemplateRecordMessageConverters() {
		this.contextRunner.withUserConfiguration(MessageConverterConfiguration.class)
			.withPropertyValues("spring.kafka.producer.transaction-id-prefix=test")
			.run((context) -> {
				KafkaTemplate<?, ?> kafkaTemplate = context.getBean(KafkaTemplate.class);
				assertThat(kafkaTemplate.getMessageConverter()).isSameAs(context.getBean("myMessageConverter"));
			});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomMessageConverter() {
		this.contextRunner.withUserConfiguration(MessageConverterConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(kafkaListenerContainerFactory).hasFieldOrPropertyWithValue("recordMessageConverter",
					context.getBean("myMessageConverter"));
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryInBatchModeWithCustomMessageConverter() {
		this.contextRunner
			.withUserConfiguration(BatchMessageConverterConfiguration.class, MessageConverterConfiguration.class)
			.withPropertyValues("spring.kafka.listener.type=batch")
			.run((context) -> {
				ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
				assertThat(kafkaListenerContainerFactory).hasFieldOrPropertyWithValue("batchMessageConverter",
						context.getBean("myBatchMessageConverter"));
			});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryInBatchModeWrapsCustomMessageConverter() {
		this.contextRunner.withUserConfiguration(MessageConverterConfiguration.class)
			.withPropertyValues("spring.kafka.listener.type=batch")
			.run((context) -> {
				ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
				Object messageConverter = ReflectionTestUtils.getField(kafkaListenerContainerFactory,
						"batchMessageConverter");
				assertThat(messageConverter).isInstanceOf(BatchMessagingMessageConverter.class);
				assertThat(((BatchMessageConverter) messageConverter).getRecordMessageConverter())
					.isSameAs(context.getBean("myMessageConverter"));
			});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryInBatchModeWithNoMessageConverter() {
		this.contextRunner.withPropertyValues("spring.kafka.listener.type=batch").run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			Object messageConverter = ReflectionTestUtils.getField(kafkaListenerContainerFactory,
					"batchMessageConverter");
			assertThat(messageConverter).isInstanceOf(BatchMessagingMessageConverter.class);
			assertThat(((BatchMessageConverter) messageConverter).getRecordMessageConverter()).isNull();
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithDefaultRecordFilterStrategy() {
		this.contextRunner.run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).hasFieldOrPropertyWithValue("recordFilterStrategy", null);
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomRecordFilterStrategy() {
		this.contextRunner.withUserConfiguration(RecordFilterStrategyConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).hasFieldOrPropertyWithValue("recordFilterStrategy",
					context.getBean("recordFilterStrategy"));
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomCommonErrorHandler() {
		this.contextRunner.withBean("errorHandler", CommonErrorHandler.class, () -> mock(CommonErrorHandler.class))
			.run((context) -> {
				ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
				assertThat(factory).hasFieldOrPropertyWithValue("commonErrorHandler", context.getBean("errorHandler"));
			});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithDefaultTransactionManager() {
		this.contextRunner.withPropertyValues("spring.kafka.producer.transaction-id-prefix=test").run((context) -> {
			assertThat(context).hasSingleBean(KafkaAwareTransactionManager.class);
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory.getContainerProperties().getTransactionManager())
				.isSameAs(context.getBean(KafkaAwareTransactionManager.class));
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void testConcurrentKafkaListenerContainerFactoryWithCustomTransactionManager() {
		KafkaTransactionManager<Object, Object> customTransactionManager = mock(KafkaTransactionManager.class);
		this.contextRunner
			.withBean("customTransactionManager", KafkaTransactionManager.class, () -> customTransactionManager)
			.withPropertyValues("spring.kafka.producer.transaction-id-prefix=test")
			.run((context) -> {
				ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
				assertThat(factory.getContainerProperties().getTransactionManager())
					.isSameAs(context.getBean("customTransactionManager"));
			});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomAfterRollbackProcessor() {
		this.contextRunner.withUserConfiguration(AfterRollbackProcessorConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).hasFieldOrPropertyWithValue("afterRollbackProcessor",
					context.getBean("afterRollbackProcessor"));
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomRecordInterceptor() {
		this.contextRunner.withUserConfiguration(RecordInterceptorConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).hasFieldOrPropertyWithValue("recordInterceptor", context.getBean("recordInterceptor"));
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomBatchInterceptor() {
		this.contextRunner.withUserConfiguration(BatchInterceptorConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).hasFieldOrPropertyWithValue("batchInterceptor", context.getBean("batchInterceptor"));
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomRebalanceListener() {
		this.contextRunner.withUserConfiguration(RebalanceListenerConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory.getContainerProperties()).hasFieldOrPropertyWithValue("consumerRebalanceListener",
					context.getBean("rebalanceListener"));
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithKafkaTemplate() {
		this.contextRunner.run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(kafkaListenerContainerFactory).hasFieldOrPropertyWithValue("replyTemplate",
					context.getBean(KafkaTemplate.class));
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomConsumerFactory() {
		this.contextRunner.withUserConfiguration(ConsumerFactoryConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(kafkaListenerContainerFactory.getConsumerFactory())
				.isNotSameAs(context.getBean(ConsumerFactoryConfiguration.class).consumerFactory);
		});
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(booleans = { true, false })
	void testConcurrentKafkaListenerContainerFactoryAutoStartup(boolean autoStartup) {
		this.contextRunner.withPropertyValues("spring.kafka.listener.auto-startup=" + autoStartup).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
				.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(kafkaListenerContainerFactory).hasFieldOrPropertyWithValue("autoStartup", autoStartup);
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomContainerCustomizer() {
		this.contextRunner.withUserConfiguration(ObservationEnabledContainerCustomizerConfiguration.class)
			.run((context) -> {
				ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
				ConcurrentMessageListenerContainer<?, ?> container = factory.createContainer("someTopic");
				assertThat(container.getContainerProperties().isObservationEnabled()).isEqualTo(true);
			});
	}

	@Test
	void specificSecurityProtocolOverridesCommonSecurityProtocol() {
		this.contextRunner
			.withPropertyValues("spring.kafka.security.protocol=SSL", "spring.kafka.admin.security.protocol=PLAINTEXT")
			.run((context) -> {
				DefaultKafkaProducerFactory<?, ?> producerFactory = context.getBean(DefaultKafkaProducerFactory.class);
				Map<String, Object> producerConfigs = producerFactory.getConfigurationProperties();
				assertThat(producerConfigs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
				KafkaAdmin admin = context.getBean(KafkaAdmin.class);
				Map<String, Object> configs = admin.getConfigurationProperties();
				assertThat(configs).containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
			});
	}

	private KafkaConnectionDetails kafkaConnectionDetails() {
		return new KafkaConnectionDetails() {

			@Override
			public List<String> getBootstrapServers() {
				return List.of("kafka.example.com:12345");
			}

		};
	}

	@Configuration(proxyBeanMethods = false)
	static class MessageConverterConfiguration {

		@Bean
		RecordMessageConverter myMessageConverter() {
			return mock(RecordMessageConverter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BatchMessageConverterConfiguration {

		@Bean
		BatchMessageConverter myBatchMessageConverter() {
			return mock(BatchMessageConverter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RecordFilterStrategyConfiguration {

		@Bean
		RecordFilterStrategy<Object, Object> recordFilterStrategy() {
			return (record) -> false;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AfterRollbackProcessorConfiguration {

		@Bean
		AfterRollbackProcessor<Object, Object> afterRollbackProcessor() {
			return (records, consumer, container, ex, recoverable, eosMode) -> {
				// no-op
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConsumerFactoryConfiguration {

		@SuppressWarnings("unchecked")
		private final ConsumerFactory<String, Object> consumerFactory = mock(ConsumerFactory.class);

		@Bean
		ConsumerFactory<String, Object> myConsumerFactory() {
			return this.consumerFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ObservationEnabledContainerCustomizerConfiguration {

		@Bean
		ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>> myContainerCustomizer() {
			return (container) -> container.getContainerProperties().setObservationEnabled(true);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RecordInterceptorConfiguration {

		@Bean
		RecordInterceptor<Object, Object> recordInterceptor() {
			return (record, consumer) -> record;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BatchInterceptorConfiguration {

		@Bean
		BatchInterceptor<Object, Object> batchInterceptor() {
			return (batch, consumer) -> batch;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RebalanceListenerConfiguration {

		@Bean
		ConsumerAwareRebalanceListener rebalanceListener() {
			return mock(ConsumerAwareRebalanceListener.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableKafkaStreams
	static class EnableKafkaStreamsConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TestKafkaStreamsConfiguration {

		@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
		KafkaStreamsConfiguration kafkaStreamsConfiguration() {
			Map<String, Object> streamsProperties = new HashMap<>();
			streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094, localhost:9095");
			streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-id");

			return new KafkaStreamsConfiguration(streamsProperties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestStreamsBuilderFactoryBeanConfiguration {

		@Bean
		StreamsBuilderFactoryBean firstStreamsBuilderFactoryBean() {
			return mock(StreamsBuilderFactoryBean.class);
		}

		@Bean
		StreamsBuilderFactoryBean secondStreamsBuilderFactoryBean() {
			return mock(StreamsBuilderFactoryBean.class);
		}

	}

}
