/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.AppConfigurationEntry;

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
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.SeekToCurrentBatchErrorHandler;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.ChainedKafkaTransactionManager;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link KafkaAutoConfiguration}.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Nakul Mishra
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
				"spring.kafka.consumer.key-deserializer = org.apache.kafka.common.serialization.LongDeserializer",
				"spring.kafka.consumer.value-deserializer = org.apache.kafka.common.serialization.IntegerDeserializer")
				.run((context) -> {
					DefaultKafkaConsumerFactory<?, ?> consumerFactory = context
							.getBean(DefaultKafkaConsumerFactory.class);
					Map<String, Object> configs = consumerFactory.getConfigurationProperties();
					// common
					assertThat(configs.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))
							.isEqualTo(Collections.singletonList("foo:1234"));
					assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p1");
					assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "ksLoc");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p2");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "tsLoc");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p3");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat(configs.get(SslConfigs.SSL_PROTOCOL_CONFIG)).isEqualTo("TLSv1.2");
					// consumer
					assertThat(configs.get(ConsumerConfig.CLIENT_ID_CONFIG)).isEqualTo("ccid"); // override
					assertThat(configs.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG)).isEqualTo(Boolean.FALSE);
					assertThat(configs.get(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG)).isEqualTo(123);
					assertThat(configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)).isEqualTo("earliest");
					assertThat(configs.get(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG)).isEqualTo(456);
					assertThat(configs.get(ConsumerConfig.FETCH_MIN_BYTES_CONFIG)).isEqualTo(1024);
					assertThat(configs.get(ConsumerConfig.GROUP_ID_CONFIG)).isEqualTo("bar");
					assertThat(configs.get(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG)).isEqualTo(234);
					assertThat(configs.get(ConsumerConfig.ISOLATION_LEVEL_CONFIG)).isEqualTo("read_committed");
					assertThat(configs.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG))
							.isEqualTo(LongDeserializer.class);
					assertThat(configs.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG))
							.isEqualTo(IntegerDeserializer.class);
					assertThat(configs.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG)).isEqualTo(42);
					assertThat(configs.get("foo")).isEqualTo("bar");
					assertThat(configs.get("baz")).isEqualTo("qux");
					assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
					assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
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
				"spring.kafka.producer.ssl.key-password=p4",
				"spring.kafka.producer.ssl.key-store-location=classpath:ksLocP",
				"spring.kafka.producer.ssl.key-store-password=p5", "spring.kafka.producer.ssl.key-store-type=PKCS12",
				"spring.kafka.producer.ssl.trust-store-location=classpath:tsLocP",
				"spring.kafka.producer.ssl.trust-store-password=p6",
				"spring.kafka.producer.ssl.trust-store-type=PKCS12", "spring.kafka.producer.ssl.protocol=TLSv1.2",
				"spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.IntegerSerializer")
				.run((context) -> {
					DefaultKafkaProducerFactory<?, ?> producerFactory = context
							.getBean(DefaultKafkaProducerFactory.class);
					Map<String, Object> configs = producerFactory.getConfigurationProperties();
					// common
					assertThat(configs.get(ProducerConfig.CLIENT_ID_CONFIG)).isEqualTo("cid");
					// producer
					assertThat(configs.get(ProducerConfig.ACKS_CONFIG)).isEqualTo("all");
					assertThat(configs.get(ProducerConfig.BATCH_SIZE_CONFIG)).isEqualTo(2048);
					assertThat(configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
							.isEqualTo(Collections.singletonList("bar:1234")); // override
					assertThat(configs.get(ProducerConfig.BUFFER_MEMORY_CONFIG)).isEqualTo(4096L);
					assertThat(configs.get(ProducerConfig.COMPRESSION_TYPE_CONFIG)).isEqualTo("gzip");
					assertThat(configs.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(LongSerializer.class);
					assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p4");
					assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "ksLocP");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p5");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "tsLocP");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p6");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat(configs.get(SslConfigs.SSL_PROTOCOL_CONFIG)).isEqualTo("TLSv1.2");
					assertThat(configs.get(ProducerConfig.RETRIES_CONFIG)).isEqualTo(2);
					assertThat(configs.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG))
							.isEqualTo(IntegerSerializer.class);
					assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).isEmpty();
					assertThat(context.getBeansOfType(KafkaTransactionManager.class)).isEmpty();
					assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
					assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
				});
	}

	@Test
	void adminProperties() {
		this.contextRunner
				.withPropertyValues("spring.kafka.clientId=cid", "spring.kafka.properties.foo.bar.baz=qux.fiz.buz",
						"spring.kafka.admin.fail-fast=true", "spring.kafka.admin.properties.fiz.buz=fix.fox",
						"spring.kafka.admin.ssl.key-password=p4",
						"spring.kafka.admin.ssl.key-store-location=classpath:ksLocP",
						"spring.kafka.admin.ssl.key-store-password=p5", "spring.kafka.admin.ssl.key-store-type=PKCS12",
						"spring.kafka.admin.ssl.trust-store-location=classpath:tsLocP",
						"spring.kafka.admin.ssl.trust-store-password=p6",
						"spring.kafka.admin.ssl.trust-store-type=PKCS12", "spring.kafka.admin.ssl.protocol=TLSv1.2")
				.run((context) -> {
					KafkaAdmin admin = context.getBean(KafkaAdmin.class);
					Map<String, Object> configs = admin.getConfig();
					// common
					assertThat(configs.get(AdminClientConfig.CLIENT_ID_CONFIG)).isEqualTo("cid");
					// admin
					assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p4");
					assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "ksLocP");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p5");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "tsLocP");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p6");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat(configs.get(SslConfigs.SSL_PROTOCOL_CONFIG)).isEqualTo("TLSv1.2");
					assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).isEmpty();
					assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
					assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
					assertThat(admin).hasFieldOrPropertyWithValue("fatalIfBrokerNotAvailable", true);
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	void streamsProperties() {
		this.contextRunner.withUserConfiguration(EnableKafkaStreamsConfiguration.class).withPropertyValues(
				"spring.kafka.client-id=cid", "spring.kafka.bootstrap-servers=localhost:9092,localhost:9093",
				"spring.application.name=appName", "spring.kafka.properties.foo.bar.baz=qux.fiz.buz",
				"spring.kafka.streams.auto-startup=false", "spring.kafka.streams.cache-max-size-buffering=1KB",
				"spring.kafka.streams.client-id=override", "spring.kafka.streams.properties.fiz.buz=fix.fox",
				"spring.kafka.streams.replication-factor=2", "spring.kafka.streams.state-dir=/tmp/state",
				"spring.kafka.streams.ssl.key-password=p7",
				"spring.kafka.streams.ssl.key-store-location=classpath:ksLocP",
				"spring.kafka.streams.ssl.key-store-password=p8", "spring.kafka.streams.ssl.key-store-type=PKCS12",
				"spring.kafka.streams.ssl.trust-store-location=classpath:tsLocP",
				"spring.kafka.streams.ssl.trust-store-password=p9", "spring.kafka.streams.ssl.trust-store-type=PKCS12",
				"spring.kafka.streams.ssl.protocol=TLSv1.2").run((context) -> {
					Properties configs = context
							.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME,
									KafkaStreamsConfiguration.class)
							.asProperties();
					assertThat((List<String>) configs.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
							.containsExactly("localhost:9092", "localhost:9093");
					assertThat(configs.get(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG)).isEqualTo(1024);
					assertThat(configs.get(StreamsConfig.CLIENT_ID_CONFIG)).isEqualTo("override");
					assertThat(configs.get(StreamsConfig.REPLICATION_FACTOR_CONFIG)).isEqualTo(2);
					assertThat(configs.get(StreamsConfig.STATE_DIR_CONFIG)).isEqualTo("/tmp/state");
					assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p7");
					assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "ksLocP");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p8");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "tsLocP");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p9");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat(configs.get(SslConfigs.SSL_PROTOCOL_CONFIG)).isEqualTo("TLSv1.2");
					assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).isEmpty();
					assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
					assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
					assertThat(context.getBean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME))
							.isNotNull();
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
					assertThat(configs.get(StreamsConfig.APPLICATION_ID_CONFIG)).isEqualTo("my-test-app");
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
					assertThat(configs.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
							.isEqualTo("localhost:9094, localhost:9095");
					assertThat(configs.get(StreamsConfig.APPLICATION_ID_CONFIG)).isEqualTo("test-id");
				});
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
					verify(context.getBean("&firstStreamsBuilderFactoryBean", StreamsBuilderFactoryBean.class), never())
							.setAutoStartup(false);
					verify(context.getBean("&secondStreamsBuilderFactoryBean", StreamsBuilderFactoryBean.class),
							never()).setAutoStartup(false);
				});
	}

	@Test
	void streamsApplicationIdIsMandatory() {
		this.contextRunner.withUserConfiguration(EnableKafkaStreamsConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().hasMessageContaining("spring.kafka.streams.application-id")
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

	@SuppressWarnings("unchecked")
	@Test
	void listenerProperties() {
		this.contextRunner
				.withPropertyValues("spring.kafka.template.default-topic=testTopic",
						"spring.kafka.listener.ack-mode=MANUAL", "spring.kafka.listener.client-id=client",
						"spring.kafka.listener.ack-count=123", "spring.kafka.listener.ack-time=456",
						"spring.kafka.listener.concurrency=3", "spring.kafka.listener.poll-timeout=2000",
						"spring.kafka.listener.no-poll-threshold=2.5", "spring.kafka.listener.type=batch",
						"spring.kafka.listener.idle-event-interval=1s", "spring.kafka.listener.monitor-interval=45",
						"spring.kafka.listener.log-container-config=true",
						"spring.kafka.listener.missing-topics-fatal=true", "spring.kafka.jaas.enabled=true",
						"spring.kafka.producer.transaction-id-prefix=foo", "spring.kafka.jaas.login-module=foo",
						"spring.kafka.jaas.control-flag=REQUISITE", "spring.kafka.jaas.options.useKeyTab=true")
				.run((context) -> {
					DefaultKafkaProducerFactory<?, ?> producerFactory = context
							.getBean(DefaultKafkaProducerFactory.class);
					DefaultKafkaConsumerFactory<?, ?> consumerFactory = context
							.getBean(DefaultKafkaConsumerFactory.class);
					KafkaTemplate<?, ?> kafkaTemplate = context.getBean(KafkaTemplate.class);
					AbstractKafkaListenerContainerFactory<?, ?, ?> kafkaListenerContainerFactory = (AbstractKafkaListenerContainerFactory<?, ?, ?>) context
							.getBean(KafkaListenerContainerFactory.class);
					assertThat(kafkaTemplate.getMessageConverter()).isInstanceOf(MessagingMessageConverter.class);
					assertThat(kafkaTemplate).hasFieldOrPropertyWithValue("producerFactory", producerFactory);
					assertThat(kafkaTemplate.getDefaultTopic()).isEqualTo("testTopic");
					assertThat(kafkaListenerContainerFactory.getConsumerFactory()).isEqualTo(consumerFactory);
					ContainerProperties containerProperties = kafkaListenerContainerFactory.getContainerProperties();
					assertThat(containerProperties.getAckMode()).isEqualTo(AckMode.MANUAL);
					assertThat(containerProperties.getClientId()).isEqualTo("client");
					assertThat(containerProperties.getAckCount()).isEqualTo(123);
					assertThat(containerProperties.getAckTime()).isEqualTo(456L);
					assertThat(containerProperties.getPollTimeout()).isEqualTo(2000L);
					assertThat(containerProperties.getNoPollThreshold()).isEqualTo(2.5f);
					assertThat(containerProperties.getIdleEventInterval()).isEqualTo(1000L);
					assertThat(containerProperties.getMonitorInterval()).isEqualTo(45);
					assertThat(containerProperties.isLogContainerConfig()).isTrue();
					assertThat(containerProperties.isMissingTopicsFatal()).isTrue();
					assertThat(ReflectionTestUtils.getField(kafkaListenerContainerFactory, "concurrency")).isEqualTo(3);
					assertThat(kafkaListenerContainerFactory.isBatchListener()).isTrue();
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
				.withPropertyValues("spring.kafka.producer.transaction-id-prefix=test").run((context) -> {
					KafkaTemplate<?, ?> kafkaTemplate = context.getBean(KafkaTemplate.class);
					assertThat(kafkaTemplate.getMessageConverter()).isSameAs(context.getBean("myMessageConverter"));
				});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomMessageConverter() {
		this.contextRunner.withUserConfiguration(MessageConverterConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(kafkaListenerContainerFactory).hasFieldOrPropertyWithValue("messageConverter",
					context.getBean("myMessageConverter"));
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryInBatchModeWithCustomMessageConverter() {
		this.contextRunner
				.withUserConfiguration(BatchMessageConverterConfiguration.class, MessageConverterConfiguration.class)
				.withPropertyValues("spring.kafka.listener.type=batch").run((context) -> {
					ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
							.getBean(ConcurrentKafkaListenerContainerFactory.class);
					assertThat(kafkaListenerContainerFactory).hasFieldOrPropertyWithValue("messageConverter",
							context.getBean("myBatchMessageConverter"));
				});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryInBatchModeWrapsCustomMessageConverter() {
		this.contextRunner.withUserConfiguration(MessageConverterConfiguration.class)
				.withPropertyValues("spring.kafka.listener.type=batch").run((context) -> {
					ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
							.getBean(ConcurrentKafkaListenerContainerFactory.class);
					Object messageConverter = ReflectionTestUtils.getField(kafkaListenerContainerFactory,
							"messageConverter");
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
			Object messageConverter = ReflectionTestUtils.getField(kafkaListenerContainerFactory, "messageConverter");
			assertThat(messageConverter).isInstanceOf(BatchMessagingMessageConverter.class);
			assertThat(((BatchMessageConverter) messageConverter).getRecordMessageConverter()).isNull();
		});
	}

	@Test
	void testConcurrentKafkaListenerContainerFactoryWithCustomErrorHandler() {
		this.contextRunner.withUserConfiguration(ErrorHandlerConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).hasFieldOrPropertyWithValue("errorHandler", context.getBean("errorHandler"));
		});
	}

	@Test
	void concurrentKafkaListenerContainerFactoryInBatchModeShouldUseBatchErrorHandler() {
		this.contextRunner.withUserConfiguration(BatchErrorHandlerConfiguration.class)
				.withPropertyValues("spring.kafka.listener.type=batch").run((context) -> {
					ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
							.getBean(ConcurrentKafkaListenerContainerFactory.class);
					assertThat(factory).hasFieldOrPropertyWithValue("errorHandler",
							context.getBean("batchErrorHandler"));
				});
	}

	@Test
	void concurrentKafkaListenerContainerFactoryInBatchModeWhenBatchErrorHandlerNotAvailableShouldBeNull() {
		this.contextRunner.withPropertyValues("spring.kafka.listener.type=batch").run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
			assertThat(factory).hasFieldOrPropertyWithValue("errorHandler", null);
		});
	}

	@Test
	void concurrentKafkaListenerContainerFactoryInBatchModeAndSimpleErrorHandlerShouldBeNull() {
		this.contextRunner.withPropertyValues("spring.kafka.listener.type=batch")
				.withUserConfiguration(ErrorHandlerConfiguration.class).run((context) -> {
					ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
							.getBean(ConcurrentKafkaListenerContainerFactory.class);
					assertThat(factory).hasFieldOrPropertyWithValue("errorHandler", null);
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
	void testConcurrentKafkaListenerContainerFactoryWithCustomTransactionManager() {
		this.contextRunner.withUserConfiguration(TransactionManagerConfiguration.class)
				.withPropertyValues("spring.kafka.producer.transaction-id-prefix=test").run((context) -> {
					ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
							.getBean(ConcurrentKafkaListenerContainerFactory.class);
					assertThat(factory.getContainerProperties().getTransactionManager())
							.isSameAs(context.getBean("chainedTransactionManager"));
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
	static class ErrorHandlerConfiguration {

		@Bean
		SeekToCurrentErrorHandler errorHandler() {
			return new SeekToCurrentErrorHandler();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BatchErrorHandlerConfiguration {

		@Bean
		SeekToCurrentBatchErrorHandler batchErrorHandler() {
			return new SeekToCurrentBatchErrorHandler();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TransactionManagerConfiguration {

		@Bean
		@Primary
		PlatformTransactionManager chainedTransactionManager(
				KafkaTransactionManager<String, String> kafkaTransactionManager) {
			return new ChainedKafkaTransactionManager<String, String>(kafkaTransactionManager);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AfterRollbackProcessorConfiguration {

		@Bean
		AfterRollbackProcessor<Object, Object> afterRollbackProcessor() {
			return (records, consumer, ex, recoverable) -> {
				// no-op
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConsumerFactoryConfiguration {

		private final ConsumerFactory<String, Object> consumerFactory = mock(ConsumerFactory.class);

		@Bean
		ConsumerFactory<String, Object> myConsumerFactory() {
			return this.consumerFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RecordInterceptorConfiguration {

		@Bean
		RecordInterceptor<Object, Object> recordInterceptor() {
			return (record) -> record;
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
