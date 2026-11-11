/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfluentKafkaContainerConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = { "spring.kafka.consumer.group-id=test-group",
		"spring.kafka.consumer.auto-offset-reset=earliest" })
class ConfluentKafkaContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final ConfluentKafkaContainer kafka = TestImage.forContainer(ConfluentKafkaContainer.class);

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private TestListener listener;

	@Test
	void connectionCanBeMadeToKafkaContainer() {
		this.kafkaTemplate.send("test-topic", "test-data");
		Awaitility.waitAtMost(Duration.ofMinutes(4))
			.untilAsserted(() -> assertThat(this.listener.messages).containsExactly("test-data"));
	}

	@Configuration(proxyBeanMethods = false)
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
