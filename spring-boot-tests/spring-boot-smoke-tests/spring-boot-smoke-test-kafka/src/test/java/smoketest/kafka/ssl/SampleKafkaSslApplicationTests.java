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

package smoketest.kafka.ssl;

import java.io.File;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import smoketest.kafka.Consumer;
import smoketest.kafka.Producer;
import smoketest.kafka.SampleMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

@Testcontainers
@SpringBootTest(classes = { SampleKafkaSslApplication.class, Producer.class, Consumer.class },
		properties = { "spring.kafka.security.protocol=SSL", "spring.kafka.bootstrap-servers=localhost:9093",
				"spring.kafka.ssl.bundle=client",
				"spring.ssl.bundle.jks.client.keystore.location=classpath:ssl/test-client.p12",
				"spring.ssl.bundle.jks.client.keystore.password=password",
				"spring.ssl.bundle.jks.client.truststore.location=classpath:ssl/test-ca.p12",
				"spring.ssl.bundle.jks.client.truststore.password=password" })
class SampleKafkaSslApplicationTests {

	private static final File KAFKA_COMPOSE_FILE = new File("src/test/resources/docker-compose.yml");

	private static final String KAFKA_COMPOSE_SERVICE = "kafka";

	private static final int KAFKA_SSL_PORT = 9093;

	@Container
	public DockerComposeContainer<?> container = new DockerComposeContainer<>(KAFKA_COMPOSE_FILE)
		.withExposedService(KAFKA_COMPOSE_SERVICE, KAFKA_SSL_PORT, Wait.forListeningPorts(KAFKA_SSL_PORT));

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
