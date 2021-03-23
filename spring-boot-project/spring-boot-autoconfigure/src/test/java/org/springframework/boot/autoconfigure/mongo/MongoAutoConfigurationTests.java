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

package org.springframework.boot.autoconfigure.mongo;

import java.util.concurrent.TimeUnit;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;
import com.mongodb.event.ConnectionPoolListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoMetricsCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoMetricsConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolTagsProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author Jonatan Ivanov
 */
class MongoAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class));

	@Test
	void clientExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MongoClient.class));
	}

	@Test
	void settingsAdded() {
		this.contextRunner.withUserConfiguration(SettingsConfig.class)
				.run((context) -> assertThat(
						getSettings(context).getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS))
								.isEqualTo(300));
	}

	@Test
	void settingsAddedButNoHost() {
		this.contextRunner.withUserConfiguration(SettingsConfig.class)
				.run((context) -> assertThat(
						getSettings(context).getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS))
								.isEqualTo(300));
	}

	@Test
	void settingsSslConfig() {
		this.contextRunner.withUserConfiguration(SslSettingsConfig.class)
				.run((context) -> assertThat(getSettings(context).getSslSettings().isEnabled()).isTrue());
	}

	@Test
	void configuresSingleClient() {
		this.contextRunner.withUserConfiguration(FallbackMongoClientConfig.class)
				.run((context) -> assertThat(context).hasSingleBean(MongoClient.class));
	}

	@Test
	void customizerOverridesAutoConfig() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri:mongodb://localhost/test?appname=auto-config")
				.withUserConfiguration(SimpleCustomizerConfig.class)
				.run((context) -> assertThat(getSettings(context).getApplicationName()).isEqualTo("overridden-name"));
	}

	@Test
	void metricsBeansShouldBeCreatedIfMeterRegistryExists() {
		this.contextRunner.withUserConfiguration(MetricsConfig.class).run((context) -> assertThat(context)
				.hasSingleBean(MeterRegistry.class).hasSingleBean(MongoMetricsConnectionPoolTagsProvider.class)
				.hasSingleBean(MongoMetricsConnectionPoolListener.class)
				.hasSingleBean(MongoMetricsCommandTagsProvider.class).hasSingleBean(MongoMetricsCommandListener.class)
				.hasSingleBean(MongoConnectionPoolListenerClientSettingsBuilderCustomizer.class)
				.hasSingleBean(MongoCommandListenerClientSettingsBuilderCustomizer.class));
	}

	@Test
	void metricsBeansShouldNotBeCreatedIfMeterRegistryDoesNotExist() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(MeterRegistry.class)
				.doesNotHaveBean(MongoMetricsConnectionPoolTagsProvider.class)
				.doesNotHaveBean(MongoMetricsConnectionPoolListener.class)
				.doesNotHaveBean(MongoMetricsCommandTagsProvider.class)
				.doesNotHaveBean(MongoMetricsCommandListener.class)
				.doesNotHaveBean(MongoConnectionPoolListenerClientSettingsBuilderCustomizer.class)
				.doesNotHaveBean(MongoCommandListenerClientSettingsBuilderCustomizer.class));
	}

	@Test
	void metricsBeansShouldNotBeCreatedIfMongoMetricsDisabled() {
		this.contextRunner.withUserConfiguration(MetricsConfig.class)
				.withPropertyValues("spring.data.mongodb.metrics.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(MongoMetricsConnectionPoolTagsProvider.class)
						.doesNotHaveBean(MongoMetricsConnectionPoolListener.class)
						.doesNotHaveBean(MongoMetricsCommandTagsProvider.class)
						.doesNotHaveBean(MongoMetricsCommandListener.class)
						.doesNotHaveBean(MongoConnectionPoolListenerClientSettingsBuilderCustomizer.class)
						.doesNotHaveBean(MongoCommandListenerClientSettingsBuilderCustomizer.class));
	}

	@Test
	void mongoListenerCustomizersShouldBeCreatedIfListenersExist() {
		this.contextRunner.withUserConfiguration(MongoListenersConfig.class)
				.run((context) -> assertThat(context).doesNotHaveBean(MeterRegistry.class)
						.doesNotHaveBean(MongoMetricsConnectionPoolTagsProvider.class)
						.doesNotHaveBean(MongoMetricsConnectionPoolListener.class)
						.doesNotHaveBean(MongoMetricsCommandTagsProvider.class)
						.doesNotHaveBean(MongoMetricsCommandListener.class).hasSingleBean(ConnectionPoolListener.class)
						.hasSingleBean(CommandListener.class)
						.hasSingleBean(MongoConnectionPoolListenerClientSettingsBuilderCustomizer.class)
						.hasSingleBean(MongoCommandListenerClientSettingsBuilderCustomizer.class));
	}

	@Test
	void fallBackMetricsBeansShouldBeUsedIfTheyExists() {
		this.contextRunner.withUserConfiguration(FallbackMetricsConfig.class).run((context) -> assertThat(context)
				.hasSingleBean(MeterRegistry.class).hasSingleBean(MongoMetricsConnectionPoolTagsProvider.class)
				.hasSingleBean(MongoMetricsConnectionPoolListener.class)
				.hasSingleBean(MongoMetricsCommandTagsProvider.class).hasSingleBean(MongoMetricsCommandListener.class)
				.hasSingleBean(MongoConnectionPoolListenerClientSettingsBuilderCustomizer.class)
				.hasSingleBean(MongoCommandListenerClientSettingsBuilderCustomizer.class));
	}

	private MongoClientSettings getSettings(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(MongoClient.class);
		MongoClient client = context.getBean(MongoClient.class);
		return (MongoClientSettings) ReflectionTestUtils.getField(client, "settings");
	}

	@Configuration(proxyBeanMethods = false)
	static class SettingsConfig {

		@Bean
		MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().applyToSocketSettings(
					(socketSettings) -> socketSettings.connectTimeout(300, TimeUnit.MILLISECONDS)).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SslSettingsConfig {

		@Bean
		MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().applyToSslSettings((ssl) -> ssl.enabled(true)).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FallbackMongoClientConfig {

		@Bean
		MongoClient fallbackMongoClient() {
			return MongoClients.create();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SimpleCustomizerConfig {

		@Bean
		MongoClientSettingsBuilderCustomizer customizer() {
			return (clientSettingsBuilder) -> clientSettingsBuilder.applicationName("overridden-name");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MetricsConfig {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MongoListenersConfig {

		@Bean
		ConnectionPoolListener connectionPoolListener() {
			return new ConnectionPoolListener() {
			};
		}

		@Bean
		CommandListener commandListener() {
			return new CommandListener() {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FallbackMetricsConfig {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		MongoMetricsCommandListener mongoMetricsCommandListener(MeterRegistry meterRegistry,
				MongoMetricsCommandTagsProvider mongoMetricsCommandTagsProvider) {
			return new MongoMetricsCommandListener(meterRegistry, mongoMetricsCommandTagsProvider);
		}

		@Bean
		MongoMetricsCommandTagsProvider mongoMetricsCommandTagsProvider() {
			return new DefaultMongoMetricsCommandTagsProvider();
		}

		@Bean
		@ConditionalOnMissingBean
		MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListener(MeterRegistry meterRegistry,
				MongoMetricsConnectionPoolTagsProvider mongoMetricsConnectionPoolTagsProvider) {
			return new MongoMetricsConnectionPoolListener(meterRegistry, mongoMetricsConnectionPoolTagsProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		MongoMetricsConnectionPoolTagsProvider mongoMetricsConnectionPoolTagsProvider() {
			return new DefaultMongoMetricsConnectionPoolTagsProvider();
		}

	}

}
