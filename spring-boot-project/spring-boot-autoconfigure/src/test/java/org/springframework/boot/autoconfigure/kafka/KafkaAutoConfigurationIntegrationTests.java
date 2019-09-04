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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.condition.EmbeddedKafkaCondition;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.handler.annotation.Header;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KafkaAutoConfiguration}.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 */
@EmbeddedKafka(topics = KafkaAutoConfigurationIntegrationTests.TEST_TOPIC)
class KafkaAutoConfigurationIntegrationTests {

	static final String TEST_TOPIC = "testTopic";

	private static final String ADMIN_CREATED_TOPIC = "adminCreatedTopic";

	private AnnotationConfigApplicationContext context;

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
		KafkaTemplate<String, String> template = this.context.getBean(KafkaTemplate.class);
		template.send(TEST_TOPIC, "foo", "bar");
		Listener listener = this.context.getBean(Listener.class);
		assertThat(listener.latch.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(listener.key).isEqualTo("foo");
		assertThat(listener.received).isEqualTo("bar");

		DefaultKafkaProducerFactory producerFactory = this.context.getBean(DefaultKafkaProducerFactory.class);
		Producer producer = producerFactory.createProducer();
		assertThat(producer.partitionsFor(ADMIN_CREATED_TOPIC).size()).isEqualTo(10);
		producer.close();
	}

	@Test
	void testStreams() {
		load(KafkaStreamsConfig.class, "spring.application.name:my-app",
				"spring.kafka.bootstrap-servers:" + getEmbeddedKafkaBrokersAsString());
		assertThat(this.context.getBean(StreamsBuilderFactoryBean.class).isAutoStartup()).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[] { config }, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?>[] configs, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(configs);
		applicationContext.register(KafkaAutoConfiguration.class);
		TestPropertyValues.of(environment).applyTo(applicationContext);
		applicationContext.refresh();
		return applicationContext;
	}

	private String getEmbeddedKafkaBrokersAsString() {
		return EmbeddedKafkaCondition.getBroker().getBrokersAsString();
	}

	@Configuration(proxyBeanMethods = false)
	static class KafkaConfig {

		@Bean
		Listener listener() {
			return new Listener();
		}

		@Bean
		NewTopic adminCreated() {
			return TopicBuilder.name(ADMIN_CREATED_TOPIC).partitions(10).replicas(1).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableKafkaStreams
	static class KafkaStreamsConfig {

	}

	static class Listener {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile String received;

		private volatile String key;

		@KafkaListener(topics = TEST_TOPIC)
		void listen(String foo, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {
			this.received = foo;
			this.key = key;
			this.latch.countDown();
		}

	}

}
