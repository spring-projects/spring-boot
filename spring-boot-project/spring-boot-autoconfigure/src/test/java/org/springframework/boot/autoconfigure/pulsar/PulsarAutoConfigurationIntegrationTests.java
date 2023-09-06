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

package org.springframework.boot.autoconfigure.pulsar;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.PulsarClientException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PulsarAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class PulsarAutoConfigurationIntegrationTests {

	@Container
	private static final PulsarContainer PULSAR_CONTAINER = new PulsarContainer(DockerImageNames.pulsar())
		.withStartupAttempts(2)
		.withStartupTimeout(Duration.ofMinutes(3));

	private static final CountDownLatch LISTEN_LATCH = new CountDownLatch(1);

	private static final String TOPIC = "pacit-hello-topic";

	@DynamicPropertySource
	static void pulsarProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.pulsar.client.service-url", PULSAR_CONTAINER::getPulsarBrokerUrl);
		registry.add("spring.pulsar.admin.service-url", PULSAR_CONTAINER::getHttpServiceUrl);
	}

	@Test
	void appStartsWithAutoConfiguredSpringPulsarComponents(
			@Autowired(required = false) PulsarTemplate<String> pulsarTemplate) {
		assertThat(pulsarTemplate).isNotNull();
	}

	@Test
	void templateCanBeAccessedDuringWebRequest(@Autowired TestRestTemplate restTemplate) throws InterruptedException {
		assertThat(restTemplate.getForObject("/hello", String.class)).startsWith("Hello World -> ");
		assertThat(LISTEN_LATCH.await(5, TimeUnit.SECONDS)).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ DispatcherServletAutoConfiguration.class, ServletWebServerFactoryAutoConfiguration.class,
			WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, JacksonAutoConfiguration.class,
			PulsarAutoConfiguration.class, PulsarReactiveAutoConfiguration.class })
	@Import(TestWebController.class)
	static class TestConfiguration {

		@PulsarListener(subscriptionName = TOPIC + "-sub", topics = TOPIC)
		void listen(String ignored) {
			LISTEN_LATCH.countDown();
		}

	}

	@RestController
	static class TestWebController {

		private final PulsarTemplate<String> pulsarTemplate;

		TestWebController(PulsarTemplate<String> pulsarTemplate) {
			this.pulsarTemplate = pulsarTemplate;
		}

		@GetMapping("/hello")
		String sayHello() throws PulsarClientException {
			return "Hello World -> " + this.pulsarTemplate.send(TOPIC, "hello");
		}

	}

}
