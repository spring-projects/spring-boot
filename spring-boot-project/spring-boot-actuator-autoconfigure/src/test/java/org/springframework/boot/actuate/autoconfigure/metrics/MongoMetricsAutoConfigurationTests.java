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

package org.springframework.boot.actuate.autoconfigure.metrics;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoMetricsAutoConfiguration}.
 *
 * @author Chris Bono
 */
class MongoMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoMetricsAutoConfiguration.class));

	@Test
	void whenThereIsAMeterRegistryThenMetricsListenersAreAdded() {
		this.contextRunner.with(MetricsRun.simple())
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class)).run((context) -> {
					assertThat(context).hasSingleBean(MongoMetricsCommandListener.class);
					assertThat(getActualMongoClientSettingsUsedToConstructClient(context)).isNotNull()
							.extracting(MongoClientSettings::getCommandListeners).asList()
							.containsExactly(context.getBean(MongoMetricsCommandListener.class));
				});
	}

	@Test
	void whenThereIsNoMeterRegistryThenNoMetricsListenersAreAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.run(this::assertThatMetricsListenerNotAdded);
	}

	@Test
	void whenThereIsNoMongoClientSettingsOnClasspathThenNoMetricsListenersAreAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(MongoClientSettings.class))
				.run(this::assertThatMetricsListenerNotAdded);
	}

	@Test
	void whenThereIsNoMongoMetricsCommandListenerOnClasspathThenNoMetricsListenersAreAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(MongoMetricsCommandListener.class))
				.run(this::assertThatMetricsListenerNotAdded);

	}

	@Test
	void whenMetricsCommandListenerEnabledPropertyFalseThenNoMetricsListenersAreAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withPropertyValues("management.metrics.mongo.command-listener.enabled:false")
				.run(this::assertThatMetricsListenerNotAdded);
	}

	private void assertThatMetricsListenerNotAdded(final AssertableApplicationContext context) {
		assertThat(context).doesNotHaveBean(MongoMetricsCommandListener.class);
		assertThat(getActualMongoClientSettingsUsedToConstructClient(context)).isNotNull()
				.extracting(MongoClientSettings::getCommandListeners).asList().isEmpty();
	}

	private MongoClientSettings getActualMongoClientSettingsUsedToConstructClient(
			final AssertableApplicationContext context) {
		final MongoClient mongoClient = context.getBean(MongoClient.class);
		return (MongoClientSettings) ReflectionTestUtils.getField(mongoClient, "settings");
	}

}
