/*
 * Copyright 2012-2025 the original author or authors.
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

package smoketest.kafka.ssl;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import smoketest.kafka.Consumer;
import smoketest.kafka.Producer;
import smoketest.kafka.SampleMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.JksKeyStore;
import org.springframework.boot.testcontainers.service.connection.JksTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

/**
 * Smoke tests for Apache Kafka with SSL.
 *
 * @author Scott Frederick
 * @author Eddú Meléndez
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = { SampleKafkaSslApplication.class, Producer.class, Consumer.class })
class SampleKafkaSslApplicationTests {

	@Container
	@ServiceConnection
	@JksTrustStore(location = "classpath:ssl/test-ca.p12", password = "password")
	@JksKeyStore(location = "classpath:ssl/test-client.p12", password = "password")
	public static ConfluentKafkaContainer kafka = TestImage.container(SecureKafkaContainer.class);

	@Autowired
	private Producer producer;

	@Autowired
	private Consumer consumer;

	@Test
	void testVanillaExchange() {
		this.producer.send(new SampleMessage(1, "A simple test message"));

		Awaitility.waitAtMost(Duration.ofSeconds(30)).until(this.consumer::getMessages, not(empty()));
		assertThat(this.consumer.getMessages()).extracting("message").containsOnly("A simple test message");
	}

}
