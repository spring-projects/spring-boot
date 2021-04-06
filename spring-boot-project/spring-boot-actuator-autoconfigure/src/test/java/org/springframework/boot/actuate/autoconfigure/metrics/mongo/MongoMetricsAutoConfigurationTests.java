/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.mongo;

import java.util.List;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.event.ConnectionPoolListener;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoMetricsCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoMetricsConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolTagsProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoMetricsAutoConfiguration}.
 *
 * @author Chris Bono
 */
class MongoMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoMetricsAutoConfiguration.class));

	@Test
	void whenThereIsAMeterRegistryThenMetricsCommandListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class)).run((context) -> {
					assertThat(context).hasSingleBean(MongoMetricsCommandListener.class);
					assertThat(getActualMongoClientSettingsUsedToConstructClient(context)).isNotNull()
							.extracting(MongoClientSettings::getCommandListeners).asList()
							.containsExactly(context.getBean(MongoMetricsCommandListener.class));
					assertThat(getMongoMetricsCommandTagsProviderUsedToConstructListener(context))
							.isInstanceOf(DefaultMongoMetricsCommandTagsProvider.class);
				});
	}

	@Test
	void whenThereIsAMeterRegistryThenMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class)).run((context) -> {
					assertThat(context).hasSingleBean(MongoMetricsConnectionPoolListener.class);
					assertThat(getConnectionPoolListenersFromClient(context))
							.containsExactly(context.getBean(MongoMetricsConnectionPoolListener.class));
					assertThat(getMongoMetricsConnectionPoolTagsProviderUsedToConstructListener(context))
							.isInstanceOf(DefaultMongoMetricsConnectionPoolTagsProvider.class);
				});
	}

	@Test
	void whenThereIsNoMeterRegistryThenNoMetricsCommandListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.run((context) -> assertThatMetricsCommandListenerNotAdded());
	}

	@Test
	void whenThereIsNoMeterRegistryThenNoMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.run((context) -> assertThatMetricsConnectionPoolListenerNotAdded());
	}

	@Test
	void whenThereIsACustomMetricsCommandTagsProviderItIsUsed() {
		final MongoMetricsCommandTagsProvider customTagsProvider = mock(MongoMetricsCommandTagsProvider.class);
		this.contextRunner.with(MetricsRun.simple())
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withBean("customMongoMetricsCommandTagsProvider", MongoMetricsCommandTagsProvider.class,
						() -> customTagsProvider)
				.run((context) -> assertThat(getMongoMetricsCommandTagsProviderUsedToConstructListener(context))
						.isSameAs(customTagsProvider));
	}

	@Test
	void whenThereIsACustomMetricsConnectionPoolTagsProviderItIsUsed() {
		final MongoMetricsConnectionPoolTagsProvider customTagsProvider = mock(
				MongoMetricsConnectionPoolTagsProvider.class);
		this.contextRunner.with(MetricsRun.simple())
				.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withBean("customMongoMetricsConnectionPoolTagsProvider", MongoMetricsConnectionPoolTagsProvider.class,
						() -> customTagsProvider)
				.run((context) -> assertThat(getMongoMetricsConnectionPoolTagsProviderUsedToConstructListener(context))
						.isSameAs(customTagsProvider));
	}

	@Test
	void whenThereIsNoMongoClientSettingsOnClasspathThenNoMetricsCommandListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(MongoClientSettings.class))
				.run((context) -> assertThatMetricsCommandListenerNotAdded());
	}

	@Test
	void whenThereIsNoMongoClientSettingsOnClasspathThenNoMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(MongoClientSettings.class))
				.run((context) -> assertThatMetricsConnectionPoolListenerNotAdded());
	}

	@Test
	void whenThereIsNoMongoMetricsCommandListenerOnClasspathThenNoMetricsCommandListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(MongoMetricsCommandListener.class))
				.run((context) -> assertThatMetricsCommandListenerNotAdded());
	}

	@Test
	void whenThereIsNoMongoMetricsConnectionPoolListenerOnClasspathThenNoMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(MongoMetricsConnectionPoolListener.class))
				.run((context) -> assertThatMetricsConnectionPoolListenerNotAdded());
	}

	@Test
	void whenMetricsCommandListenerEnabledPropertyFalseThenNoMetricsCommandListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withPropertyValues("management.metrics.mongo.command.enabled:false")
				.run((context) -> assertThatMetricsCommandListenerNotAdded());
	}

	@Test
	void whenMetricsConnectionPoolListenerEnabledPropertyFalseThenNoMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
				.withPropertyValues("management.metrics.mongo.connectionpool.enabled:false")
				.run((context) -> assertThatMetricsConnectionPoolListenerNotAdded());
	}

	private ContextConsumer<AssertableApplicationContext> assertThatMetricsCommandListenerNotAdded() {
		return (context) -> {
			assertThat(context).doesNotHaveBean(MongoMetricsCommandListener.class);
			assertThat(getActualMongoClientSettingsUsedToConstructClient(context)).isNotNull()
					.extracting(MongoClientSettings::getCommandListeners).asList().isEmpty();
		};
	}

	private ContextConsumer<AssertableApplicationContext> assertThatMetricsConnectionPoolListenerNotAdded() {
		return (context) -> {
			assertThat(context).doesNotHaveBean(MongoMetricsConnectionPoolListener.class);
			assertThat(getConnectionPoolListenersFromClient(context)).isEmpty();
		};
	}

	private MongoClientSettings getActualMongoClientSettingsUsedToConstructClient(
			final AssertableApplicationContext context) {
		final MongoClient mongoClient = context.getBean(MongoClient.class);
		return (MongoClientSettings) ReflectionTestUtils.getField(mongoClient, "settings");
	}

	private List<ConnectionPoolListener> getConnectionPoolListenersFromClient(
			final AssertableApplicationContext context) {
		MongoClientSettings mongoClientSettings = getActualMongoClientSettingsUsedToConstructClient(context);
		ConnectionPoolSettings connectionPoolSettings = mongoClientSettings.getConnectionPoolSettings();
		@SuppressWarnings("unchecked")
		List<ConnectionPoolListener> listeners = (List<ConnectionPoolListener>) ReflectionTestUtils
				.getField(connectionPoolSettings, "connectionPoolListeners");
		return listeners;
	}

	private MongoMetricsCommandTagsProvider getMongoMetricsCommandTagsProviderUsedToConstructListener(
			final AssertableApplicationContext context) {
		MongoMetricsCommandListener listener = context.getBean(MongoMetricsCommandListener.class);
		return (MongoMetricsCommandTagsProvider) ReflectionTestUtils.getField(listener, "tagsProvider");
	}

	private MongoMetricsConnectionPoolTagsProvider getMongoMetricsConnectionPoolTagsProviderUsedToConstructListener(
			final AssertableApplicationContext context) {
		MongoMetricsConnectionPoolListener listener = context.getBean(MongoMetricsConnectionPoolListener.class);
		return (MongoMetricsConnectionPoolTagsProvider) ReflectionTestUtils.getField(listener, "tagsProvider");
	}

}
