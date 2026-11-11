/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.activemq.autoconfigure.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ActiveMQClassicContainerConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class ActiveMQClassicContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final ActiveMQContainer activemq = TestImage.ACTIVE_MQ_CLASSIC.container();

	@Autowired
	private JmsClient jmsClient;

	@Autowired
	private TestListener listener;

	@Test
	void connectionCanBeMadeToActiveMQContainer() {
		this.jmsClient.destination("sample.queue").send("message");
		Awaitility.waitAtMost(Duration.ofMinutes(1))
			.untilAsserted(() -> assertThat(this.listener.messages).containsExactly("message"));
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ ActiveMQAutoConfiguration.class, JmsAutoConfiguration.class })
	static class TestConfiguration {

		@Bean
		TestListener testListener() {
			return new TestListener();
		}

	}

	static class TestListener {

		private final List<String> messages = new ArrayList<>();

		@JmsListener(destination = "sample.queue")
		void processMessage(String message) {
			this.messages.add(message);
		}

	}

}
