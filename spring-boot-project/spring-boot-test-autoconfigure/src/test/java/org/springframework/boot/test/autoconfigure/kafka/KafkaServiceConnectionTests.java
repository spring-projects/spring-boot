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

package org.springframework.boot.test.autoconfigure.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.kafka.KafkaServiceConnectionTests.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaServiceConnection}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringBootTest(classes = TestConfiguration.class,
		properties = { "spring.kafka.consumer.group-id=test-group",
				"spring.kafka.consumer.auto-offset-reset=earliest" })
@Testcontainers(disabledWithoutDocker = true)
class KafkaServiceConnectionTests {

	@Container
	@KafkaServiceConnection
	static final KafkaContainer kafka = new KafkaContainer(DockerImageNames.kafka());

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	TestListener listener;

	@Test
	void connectionCanBeMadeToKafkaContainer() {
		this.kafkaTemplate.send("test-topic", "test-data");
		Awaitility.waitAtMost(Duration.ofSeconds(30))
			.untilAsserted(() -> assertThat(this.listener.messages).containsExactly("test-data"));
	}

	@ImportAutoConfiguration(KafkaAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		TestListener testListener() {
			return new TestListener();
		}

	}

	static class TestListener {

		private final List<String> messages = new ArrayList<>();

		@KafkaListener(topics = "test-topic")
		void processMessage(String message) {
			this.messages.add(message);
		}

	}

}
