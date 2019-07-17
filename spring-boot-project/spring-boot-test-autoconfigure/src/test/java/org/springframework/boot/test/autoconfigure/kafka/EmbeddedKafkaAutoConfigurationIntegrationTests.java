/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.kafka;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link EmbeddedKafkaAutoConfiguration}.
 *
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		properties = {
				"spring.kafka.consumer.group-id=testGroup",
				"spring.kafka.consumer.auto-offset-reset=earliest",
				"spring.kafka.bootstrap-servers=localhost:9093" // For the sake of working override
		})
@EmbeddedKafka(topics = EmbeddedKafkaAutoConfigurationIntegrationTests.TEST_TOPIC)
@DirtiesContext
public class EmbeddedKafkaAutoConfigurationIntegrationTests {

	static final String TEST_TOPIC = "testTopic";

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private Listener listener;

	@Autowired
	EmbeddedKafkaBroker embeddedKafkaBroker;

	@Autowired
	private KafkaProperties kafkaProperties;

	@Test
	public void testEndToEnd() throws Exception {
		assertThat(String.join(",", this.kafkaProperties.getBootstrapServers()))
				.isEqualTo(this.embeddedKafkaBroker.getBrokersAsString());
		this.kafkaTemplate.send(TEST_TOPIC, "bar");
		assertThat(this.listener.latch.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(this.listener.received).isEqualTo("bar");
	}

	@Configuration
	@ImportAutoConfiguration({ KafkaAutoConfiguration.class,
			EmbeddedKafkaAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	static class KafkaConfig {

		@Bean
		public Listener listener() {
			return new Listener();
		}

	}

	public static class Listener {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile String received;

		@KafkaListener(topics = TEST_TOPIC)
		public void listen(String foo) {
			this.received = foo;
			this.latch.countDown();
		}

	}

}
