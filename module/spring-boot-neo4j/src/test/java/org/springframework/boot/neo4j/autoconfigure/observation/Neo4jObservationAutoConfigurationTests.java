/*
 * Copyright 2012-present the original author or authors.
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
