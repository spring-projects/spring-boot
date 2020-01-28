/*
 * Copyright 2012-2019 the original author or authors.
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

import javax.net.SocketFactory;

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
import static org.mockito.Mockito.mock;

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
	void optionsAdded() {
		this.contextRunner.withUserConfiguration(OptionsConfig.class)
				.run((context) -> assertThat(extractClientSettings(context.getBean(MongoClient.class))
						.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS)).isEqualTo(300));
	}

	@Test
	void optionsAddedButNoHost() {
		this.contextRunner.withUserConfiguration(OptionsConfig.class)
				.run((context) -> assertThat(extractClientSettings(context.getBean(MongoClient.class))
						.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS)).isEqualTo(300));
	}

	@Test
	void optionsSslConfig() {
		this.contextRunner.withUserConfiguration(SslOptionsConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(MongoClient.class);
			MongoClientSettings options = extractClientSettings(context.getBean(MongoClient.class));
			assertThat(options.getSslSettings().isEnabled()).isTrue();
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
	static class OptionsConfig {

		@Bean
		MongoClientSettings mongoOptions() {
			return MongoClientSettings.builder()
					.applyToSocketSettings((it) -> it.connectTimeout(300, TimeUnit.MILLISECONDS)).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SslOptionsConfig {

		@Bean
		MongoClientSettings mongoClientOptions(SocketFactory socketFactory) {
			return MongoClientSettings.builder().applyToSslSettings((it) -> it.enabled(true)).build();
		}

		@Bean
		SocketFactory mySocketFactory() {
			return mock(SocketFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FallbackMongoClientConfig {

		@Bean
		com.mongodb.client.MongoClient fallbackMongoClient() {
			return MongoClients.create();
		}

	}

}
