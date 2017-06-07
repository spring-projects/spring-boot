/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.messaging.handler.annotation.Header;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KafkaAutoConfiguration}.
 *
 * @author Gary Russell
 */
public class KafkaAutoConfigurationIntegrationTests {

	private static final String TEST_TOPIC = "testTopic";

	@ClassRule
	public static final KafkaEmbedded kafkaEmbedded = new KafkaEmbedded(1, true,
			TEST_TOPIC);

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testEndToEnd() throws Exception {
		load(KafkaConfig.class,
				"spring.kafka.bootstrap-servers:" + kafkaEmbedded.getBrokersAsString(),
				"spring.kafka.consumer.group-id=testGroup",
				"spring.kafka.consumer.auto-offset-reset=earliest");
		@SuppressWarnings("unchecked")
		KafkaTemplate<String, String> template = this.context
				.getBean(KafkaTemplate.class);
		template.send(TEST_TOPIC, "foo", "bar");
		Listener listener = this.context.getBean(Listener.class);
		assertThat(listener.latch.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(listener.key).isEqualTo("foo");
		assertThat(listener.received).isEqualTo("bar");
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[] { config }, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?>[] configs,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(configs);
		applicationContext.register(KafkaAutoConfiguration.class);
		TestPropertyValues.of(environment).applyTo(applicationContext);
		applicationContext.refresh();
		return applicationContext;
	}

	public static class KafkaConfig {

		@Bean
		public Listener listener() {
			return new Listener();
		}

	}

	public static class Listener {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile String received;

		private volatile String key;

		@KafkaListener(topics = TEST_TOPIC)
		public void listen(String foo,
				@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {
			this.received = foo;
			this.key = key;
			this.latch.countDown();
		}

	}

}
