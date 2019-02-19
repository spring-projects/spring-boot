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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
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
				"spring.kafka.consumer.auto-offset-reset=earliest"
		})
@EmbeddedKafka(controlledShutdown = true)
@DirtiesContext
public class EmbeddedKafkaAutoConfigurationIntegrationTests {

	private static final String TEST_TOPIC = "testTopic";

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private Listener listener;

	@Test
	public void testEndToEnd() throws Exception {
		this.kafkaTemplate.send(TEST_TOPIC, "foo", "bar");
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
