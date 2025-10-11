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

package org.springframework.boot.neo4j.autoconfigure.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.internal.observation.NoopObservationProvider;
import org.neo4j.driver.observation.micrometer.MicrometerObservationProvider;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jObservationAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class Neo4jObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(Neo4jObservationAutoConfiguration.class));

	@Test
	void whenThereIsAnObservationRegistryThenMicrometerObservationProviderIsAdded() {
		this.contextRunner.withBean(TestObservationRegistry.class, TestObservationRegistry::create)
			.withConfiguration(AutoConfigurations.of(Neo4jAutoConfiguration.class))
			.run((context) -> assertThat(context.getBean(Driver.class)).extracting("observationProvider")
				.isInstanceOf(MicrometerObservationProvider.class));
	}

	@Test
	void whenThereIsNoObservationRegistryThenConfigBuilderCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(Neo4jAutoConfiguration.class))
			.run((context) -> assertThat(context.getBean(Driver.class)).extracting("observationProvider")
				.isInstanceOf(NoopObservationProvider.class));
	}

}
