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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DestinationTopic;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.condition.EmbeddedKafkaCondition;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.handler.annotation.Header;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KafkaAutoConfiguration}.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Tomaz Fernandes
 * @author Andy Wilkinson
 */
@DisabledOnOs(OS.WINDOWS)
@EmbeddedKafka(topics = KafkaAutoConfigurationIntegrationTests.TEST_TOPIC)
class KafkaAutoConfigurationIntegrationTests {

	static final String TEST_TOPIC = "testTopic";
	static final String TEST_RETRY_TOPIC = "testRetryTopic";

	private static final String ADMIN_CREATED_TOPIC = "adminCreatedTopic";

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void testEndToEnd() throws Exception {
		load(KafkaConfig.class, "spring.kafka.bootstrap-servers:" + getEmbeddedKafkaBrokersAsString(),
				"spring.kafka.consumer.group-id=testGroup", "spring.kafka.consumer.auto-offset-reset=earliest");
		KafkaTemplate<String, String> template = getContext().getBean(KafkaTemplate.class);
		template.send(TEST_TOPIC, "foo", "bar");
		Listener listener = getContext().getBean(Listener.class);
		assertThat(listener.latch.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(listener.key).isEqualTo("foo");
		assertThat(listener.received).isEqualTo("bar");

		DefaultKafkaProducerFactory producerFactory = getContext().getBean(DefaultKafkaProducerFactory.class);
		Producer producer = producerFactory.createProducer();
		assertThat(producer.partitionsFor(ADMIN_CREATED_TOPIC)).hasSize(10);
		producer.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	void testEndToEndWithRetryTopics() throws Exception {
		load(KafkaConfig.class, "spring.kafka.bootstrap-servers:" + getEmbeddedKafkaBrokersAsString(),
				"spring.kafka.consumer.group-id=testGroup", "spring.kafka.retry.topic.enabled=true",
				"spring.kafka.retry.topic.attempts=5", "spring.kafka.retry.topic.backoff.delay=100ms",
				"spring.kafka.retry.topic.backoff.multiplier=2", "spring.kafka.retry.topic.backoff.max-delay=300ms",
				"spring.kafka.consumer.auto-offset-reset=earliest");
		RetryTopicConfiguration configuration = getContext().getBean(RetryTopicConfiguration.class);
		assertThat(configuration.getDestinationTopicProperties()).extracting(DestinationTopic.Properties::delay)
			.containsExactly(0L, 100L, 200L, 300L, 0L);
		KafkaTemplate<String, String> template = getContext().getBean(KafkaTemplate.class);
		template.send(TEST_RETRY_TOPIC, "foo", "bar");
		RetryListener listener = getContext().getBean(RetryListener.class);
		assertThat(listener.latch.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(listener).extracting(RetryListener::getKey, RetryListener::getReceived)
			.containsExactly("foo", "bar");
		assertThat(listener).extracting(RetryListener::getTopics)
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.hasSize(5)
			.containsSequence("testRetryTopic", "testRetryTopic-retry-0", "testRetryTopic-retry-1",
					"testRetryTopic-retry-2");
	}

	@Test
	void testStreams() {
		load(KafkaStreamsConfig.class, "spring.application.name:my-app",
				"spring.kafka.bootstrap-servers:" + getEmbeddedKafkaBrokersAsString());
		assertThat(getContext().getBean(StreamsBuilderFactoryBean.class).isAutoStartup()).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[] { config }, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?>[] configs, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(configs);
		applicationContext.register(SslAutoConfiguration.class);
		applicationContext.register(KafkaAutoConfiguration.class);
		TestPropertyValues.of(environment).applyTo(applicationContext);
		applicationContext.refresh();
		return applicationContext;
	}

	private String getEmbeddedKafkaBrokersAsString() {
		EmbeddedKafkaBroker broker = EmbeddedKafkaCondition.getBroker();
		assertThat(broker).isNotNull();
		return broker.getBrokersAsString();
	}

	private AnnotationConfigApplicationContext getContext() {
		AnnotationConfigApplicationContext context = this.context;
		assertThat(context).isNotNull();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	static class KafkaConfig {

		@Bean
		Listener listener() {
			return new Listener();
		}

		@Bean
		RetryListener retryListener() {
			return new RetryListener();
		}

		@Bean
		NewTopic adminCreated() {
			return TopicBuilder.name(ADMIN_CREATED_TOPIC).partitions(10).replicas(1).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableKafkaStreams
	static class KafkaStreamsConfig {

		@Bean
		KTable<?, ?> table(StreamsBuilder builder) {
			KStream<Object, Object> stream = builder.stream(Pattern.compile("test"));
			return stream.groupByKey().count(Materialized.as("store"));
		}

	}

	static class Listener {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile @Nullable String received;

		private volatile @Nullable String key;

		@KafkaListener(topics = TEST_TOPIC)
		void listen(String foo, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
			this.received = foo;
			this.key = key;
			this.latch.countDown();
		}

	}

	static class RetryListener {

		private final CountDownLatch latch = new CountDownLatch(5);

		private final List<String> topics = new ArrayList<>();

		private volatile @Nullable String received;

		private volatile @Nullable String key;

		@KafkaListener(topics = TEST_RETRY_TOPIC)
		void listen(String foo, @Header(KafkaHeaders.RECEIVED_KEY) String key,
				@Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
			this.received = foo;
			this.key = key;
			this.topics.add(topic);
			this.latch.countDown();
			throw new RuntimeException("Test exception");
		}

		private List<String> getTopics() {
			return this.topics;
		}

		private @Nullable String getReceived() {
			return this.received;
		}

		private @Nullable String getKey() {
			return this.key;
		}

	}

}
