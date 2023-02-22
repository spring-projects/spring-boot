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

package org.springframework.boot.actuate.autoconfigure.metrics.mongo;

import java.util.List;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.event.ConnectionPoolListener;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;
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
 * @author Johnny Lim
 */
class MongoMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MongoMetricsAutoConfiguration.class));

	@Test
	void whenThereIsAMeterRegistryThenMetricsCommandListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(MongoMetricsCommandListener.class);
				assertThat(getActualMongoClientSettingsUsedToConstructClient(context))
					.extracting(MongoClientSettings::getCommandListeners)
					.asList()
					.containsExactly(context.getBean(MongoMetricsCommandListener.class));
				assertThat(getMongoCommandTagsProviderUsedToConstructListener(context))
					.isInstanceOf(DefaultMongoCommandTagsProvider.class);
			});
	}

	@Test
	void whenThereIsAMeterRegistryThenMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(MongoMetricsConnectionPoolListener.class);
				assertThat(getConnectionPoolListenersFromClient(context))
					.containsExactly(context.getBean(MongoMetricsConnectionPoolListener.class));
				assertThat(getMongoConnectionPoolTagsProviderUsedToConstructListener(context))
					.isInstanceOf(DefaultMongoConnectionPoolTagsProvider.class);
			});
	}

	@Test
	void whenThereIsNoMeterRegistryThenNoMetricsCommandListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.run(assertThatMetricsCommandListenerNotAdded());
	}

	@Test
	void whenThereIsNoMeterRegistryThenNoMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.run(assertThatMetricsConnectionPoolListenerNotAdded());
	}

	@Test
	void whenThereIsACustomMetricsCommandTagsProviderItIsUsed() {
		final MongoCommandTagsProvider customTagsProvider = mock(MongoCommandTagsProvider.class);
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withBean("customMongoCommandTagsProvider", MongoCommandTagsProvider.class, () -> customTagsProvider)
			.run((context) -> assertThat(getMongoCommandTagsProviderUsedToConstructListener(context))
				.isSameAs(customTagsProvider));
	}

	@Test
	void whenThereIsACustomMetricsConnectionPoolTagsProviderItIsUsed() {
		final MongoConnectionPoolTagsProvider customTagsProvider = mock(MongoConnectionPoolTagsProvider.class);
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withBean("customMongoConnectionPoolTagsProvider", MongoConnectionPoolTagsProvider.class,
					() -> customTagsProvider)
			.run((context) -> assertThat(getMongoConnectionPoolTagsProviderUsedToConstructListener(context))
				.isSameAs(customTagsProvider));
	}

	@Test
	void whenThereIsNoMongoClientSettingsOnClasspathThenNoMetricsCommandListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(MongoClientSettings.class))
			.run(assertThatMetricsCommandListenerNotAdded());
	}

	@Test
	void whenThereIsNoMongoClientSettingsOnClasspathThenNoMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(MongoClientSettings.class))
			.run(assertThatMetricsConnectionPoolListenerNotAdded());
	}

	@Test
	void whenThereIsNoMongoMetricsCommandListenerOnClasspathThenNoMetricsCommandListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(MongoMetricsCommandListener.class))
			.run(assertThatMetricsCommandListenerNotAdded());
	}

	@Test
	void whenThereIsNoMongoMetricsConnectionPoolListenerOnClasspathThenNoMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(MongoMetricsConnectionPoolListener.class))
			.run(assertThatMetricsConnectionPoolListenerNotAdded());
	}

	@Test
	void whenMetricsCommandListenerEnabledPropertyFalseThenNoMetricsCommandListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withPropertyValues("management.metrics.mongo.command.enabled:false")
			.run(assertThatMetricsCommandListenerNotAdded());
	}

	@Test
	void whenMetricsConnectionPoolListenerEnabledPropertyFalseThenNoMetricsConnectionPoolListenerIsAdded() {
		this.contextRunner.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withPropertyValues("management.metrics.mongo.connectionpool.enabled:false")
			.run(assertThatMetricsConnectionPoolListenerNotAdded());
	}

	private ContextConsumer<AssertableApplicationContext> assertThatMetricsCommandListenerNotAdded() {
		return (context) -> {
			assertThat(context).doesNotHaveBean(MongoMetricsCommandListener.class);
			assertThat(getActualMongoClientSettingsUsedToConstructClient(context))
				.extracting(MongoClientSettings::getCommandListeners)
				.asList()
				.isEmpty();
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
		final MongoClientImpl mongoClient = (MongoClientImpl) context.getBean(MongoClient.class);
		return mongoClient.getSettings();
	}

	private List<ConnectionPoolListener> getConnectionPoolListenersFromClient(
			final AssertableApplicationContext context) {
		MongoClientSettings mongoClientSettings = getActualMongoClientSettingsUsedToConstructClient(context);
		ConnectionPoolSettings connectionPoolSettings = mongoClientSettings.getConnectionPoolSettings();
		return connectionPoolSettings.getConnectionPoolListeners();
	}

	private MongoCommandTagsProvider getMongoCommandTagsProviderUsedToConstructListener(
			final AssertableApplicationContext context) {
		MongoMetricsCommandListener listener = context.getBean(MongoMetricsCommandListener.class);
		return (MongoCommandTagsProvider) ReflectionTestUtils.getField(listener, "tagsProvider");
	}

	private MongoConnectionPoolTagsProvider getMongoConnectionPoolTagsProviderUsedToConstructListener(
			final AssertableApplicationContext context) {
		MongoMetricsConnectionPoolListener listener = context.getBean(MongoMetricsConnectionPoolListener.class);
		return (MongoConnectionPoolTagsProvider) ReflectionTestUtils.getField(listener, "tagsProvider");
	}

}
