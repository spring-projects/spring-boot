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
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
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
				.run((context) -> assertThat(extractClientSettings(context.getBean(MongoClient.class))
						.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS)).isEqualTo(300));
	}

	@Test
	void settingsAddedButNoHost() {
		this.contextRunner.withUserConfiguration(SettingsConfig.class)
				.run((context) -> assertThat(extractClientSettings(context.getBean(MongoClient.class))
						.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS)).isEqualTo(300));
	}

	@Test
	void settingsSslConfig() {
		this.contextRunner.withUserConfiguration(SslSettingsConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(MongoClient.class);
			MongoClientSettings settings = extractClientSettings(context.getBean(MongoClient.class));
			assertThat(settings.getSslSettings().isEnabled()).isTrue();
		});
	}

	@Test
	void configuresSingleClient() {
		this.contextRunner.withUserConfiguration(FallbackMongoClientConfig.class)
				.run((context) -> assertThat(context).hasSingleBean(MongoClient.class));
	}

	private static MongoClientSettings extractClientSettings(MongoClient client) {
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

}
