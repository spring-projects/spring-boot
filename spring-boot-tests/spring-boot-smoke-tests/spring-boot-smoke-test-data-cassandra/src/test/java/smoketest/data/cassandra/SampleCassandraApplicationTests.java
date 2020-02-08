/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.data.cassandra;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleCassandraApplication}.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = SampleCassandraApplicationTests.Initializer.class)
@Testcontainers(disabledWithoutDocker = true)
class SampleCassandraApplicationTests {

	@Container
	static final CassandraContainer<?> container = new CassandraContainer<>().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10)).withInitScript("setup.cql");

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void testDefaultSettings(CapturedOutput output) {
		assertThat(output).contains("firstName='Alice', lastName='Smith'");
	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertyValues
					.of("spring.data.cassandra.contact-points:localhost:" + container.getFirstMappedPort(),
							"spring.data.cassandra.local-datacenter=datacenter1",
							"spring.data.cassandra.read-timeout=20s", "spring.data.cassandra.connect-timeout=10s")
					.applyTo(configurableApplicationContext.getEnvironment());
		}

	}

}
