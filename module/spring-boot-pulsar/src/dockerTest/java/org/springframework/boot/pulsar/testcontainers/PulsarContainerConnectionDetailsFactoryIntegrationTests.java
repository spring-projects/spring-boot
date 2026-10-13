/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.pulsar.PulsarContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.pulsar.autoconfigure.PulsarAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PulsarContainerConnectionDetailsFactory}.
 *
 * @author Chris Bono
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = { "spring.pulsar.consumer.subscription.initial-position=earliest" })
class PulsarContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	@SuppressWarnings("unused")
	static final PulsarContainer pulsar = TestImage.forContainer(PulsarContainer.class);

	@Autowired
	private PulsarTemplate<String> pulsarTemplate;

	@Autowired
	private TestListener listener;

	@Test
	void connectionCanBeMadeToPulsarContainer() {
		this.pulsarTemplate.send("test-topic", "test-data");
		Awaitility.waitAtMost(Duration.ofSeconds(30))
			.untilAsserted(() -> assertThat(this.listener.messages).containsExactly("test-data"));
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(PulsarAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		TestListener testListener() {
			return new TestListener();
		}

	}

	static class TestListener {

		private final List<String> messages = new ArrayList<>();

		@PulsarListener(topics = "test-topic")
		void processMessage(String message) {
			this.messages.add(message);
		}

	}

}
