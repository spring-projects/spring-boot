/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.pulsar.autoconfigure.PulsarAutoConfiguration;
import org.springframework.boot.pulsar.autoconfigure.PulsarReactiveAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PulsarAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PulsarAutoConfigurationIntegrationTests {

	@Container
	static final PulsarContainer pulsar = TestImage.container(PulsarContainer.class);

	private static final CountDownLatch listenLatch = new CountDownLatch(1);

	private static final String TOPIC = "pacit-hello-topic";

	@DynamicPropertySource
	static void pulsarProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.pulsar.client.service-url", pulsar::getPulsarBrokerUrl);
		registry.add("spring.pulsar.admin.service-url", pulsar::getHttpServiceUrl);
	}

	@Test
	void appStartsWithAutoConfiguredSpringPulsarComponents(
			@Autowired(required = false) PulsarTemplate<String> pulsarTemplate) {
		assertThat(pulsarTemplate).isNotNull();
	}

	@Test
	void sendAndReceive(@Autowired TestService testService) throws InterruptedException {
		assertThat(testService.sayHello()).startsWith("Hello World -> ");
		assertThat(listenLatch.await(5, TimeUnit.SECONDS)).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ PulsarAutoConfiguration.class, PulsarReactiveAutoConfiguration.class })
	@Import(TestService.class)
	static class TestConfiguration {

		@PulsarListener(subscriptionName = TOPIC + "-sub", topics = TOPIC)
		void listen(String ignored) {
			listenLatch.countDown();
		}

	}

	@Service
	static class TestService {

		private final PulsarTemplate<String> pulsarTemplate;

		TestService(PulsarTemplate<String> pulsarTemplate) {
			this.pulsarTemplate = pulsarTemplate;
		}

		String sayHello() {
			return "Hello World -> " + this.pulsarTemplate.send(TOPIC, "hello");
		}

	}

}
