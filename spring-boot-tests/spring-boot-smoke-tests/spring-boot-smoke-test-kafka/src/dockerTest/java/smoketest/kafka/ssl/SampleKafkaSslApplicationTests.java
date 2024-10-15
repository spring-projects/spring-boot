/*
 * Copyright 2012-2024 the original author or authors.
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
import org.testcontainers.utility.MountableFile;
import smoketest.kafka.Consumer;
import smoketest.kafka.Producer;
import smoketest.kafka.SampleMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
@SpringBootTest(classes = { SampleKafkaSslApplication.class, Producer.class, Consumer.class },
		properties = { "spring.kafka.security.protocol=SSL",
				"spring.kafka.properties.ssl.endpoint.identification.algorithm=", "spring.kafka.ssl.bundle=client",
				"spring.ssl.bundle.jks.client.keystore.location=classpath:ssl/test-client.p12",
				"spring.ssl.bundle.jks.client.keystore.password=password",
				"spring.ssl.bundle.jks.client.truststore.location=classpath:ssl/test-ca.p12",
				"spring.ssl.bundle.jks.client.truststore.password=password" })
class SampleKafkaSslApplicationTests {

	@Container
	public static ConfluentKafkaContainer kafka = TestImage.container(ConfluentKafkaContainer.class)
		.withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SSL,BROKER:PLAINTEXT,CONTROLLER:PLAINTEXT")
		.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
		.withEnv("KAFKA_SSL_CLIENT_AUTH", "required")
		.withEnv("KAFKA_SSL_KEYSTORE_LOCATION", "/etc/kafka/secrets/certs/test-server.p12")
		.withEnv("KAFKA_SSL_KEYSTORE_PASSWORD", "password")
		.withEnv("KAFKA_SSL_KEY_PASSWORD", "password")
		.withEnv("KAFKA_SSL_TRUSTSTORE_LOCATION", "/etc/kafka/secrets/certs/test-ca.p12")
		.withEnv("KAFKA_SSL_TRUSTSTORE_PASSWORD", "password")
		.withEnv("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "")
		.withCopyFileToContainer(MountableFile.forClasspathResource("ssl/test-server.p12"),
				"/etc/kafka/secrets/certs/test-server.p12")
		.withCopyFileToContainer(MountableFile.forClasspathResource("ssl/credentials"),
				"/etc/kafka/secrets/certs/credentials")
		.withCopyFileToContainer(MountableFile.forClasspathResource("ssl/test-ca.p12"),
				"/etc/kafka/secrets/certs/test-ca.p12");

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers",
				() -> String.format("%s:%s", kafka.getHost(), kafka.getMappedPort(9092)));
	}

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
