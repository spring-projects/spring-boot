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

package smoketest.pulsar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(OutputCaptureExtension.class)
class SamplePulsarApplicationTests {

	@Container
	static final PulsarContainer PULSAR_CONTAINER = new PulsarContainer(DockerImageNames.pulsar())
		.withStartupAttempts(2)
		.withStartupTimeout(Duration.ofMinutes(3));

	@DynamicPropertySource
	static void pulsarProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.pulsar.client.service-url", PULSAR_CONTAINER::getPulsarBrokerUrl);
		registry.add("spring.pulsar.admin.service-url", PULSAR_CONTAINER::getHttpServiceUrl);
	}

	@Nested
	@SpringBootTest
	@ActiveProfiles("smoketest.pulsar.imperative")
	class ImperativeApp {

		@Test
		void appProducesAndConsumesMessages(CapturedOutput output) {
			List<String> expectedOutput = new ArrayList<>();
			IntStream.range(0, 10).forEachOrdered((i) -> {
				expectedOutput.add("++++++PRODUCE IMPERATIVE:(" + i + ")------");
				expectedOutput.add("++++++CONSUME IMPERATIVE:(" + i + ")------");
			});
			Awaitility.waitAtMost(Duration.ofSeconds(30))
				.untilAsserted(() -> assertThat(output).contains(expectedOutput));
		}

	}

	@Nested
	@SpringBootTest
	@ActiveProfiles("smoketest.pulsar.reactive")
	class ReactiveApp {

		@Test
		void appProducesAndConsumesMessagesReactively(CapturedOutput output) {
			List<String> expectedOutput = new ArrayList<>();
			IntStream.range(0, 10).forEachOrdered((i) -> {
				expectedOutput.add("++++++PRODUCE REACTIVE:(" + i + ")------");
				expectedOutput.add("++++++CONSUME REACTIVE:(" + i + ")------");
			});
			Awaitility.waitAtMost(Duration.ofSeconds(30))
				.untilAsserted(() -> assertThat(output).contains(expectedOutput));
		}

	}

}
