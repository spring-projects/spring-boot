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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.MongoClientSettings;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MongoClientFactorySupport}.
 *
 * @param <T> the mongo client type
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @author Scott Frederick
 */
abstract class MongoClientFactorySupportTests<T> {

	@Test
	void canBindCharArrayPassword() {
		// gh-1572
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.password:word").applyTo(context);
		context.register(Config.class);
		context.refresh();
		MongoProperties properties = context.getBean(MongoProperties.class);
		assertThat(properties.getPassword()).isEqualTo("word".toCharArray());
	}

	@Test
	void allMongoClientSettingsCanBeSet() {
		MongoClientSettings.Builder builder = MongoClientSettings.builder();
		builder.applyToSocketSettings((settings) -> {
			settings.connectTimeout(1000, TimeUnit.MILLISECONDS);
			settings.readTimeout(1000, TimeUnit.MILLISECONDS);
		}).applyToServerSettings((settings) -> {
			settings.heartbeatFrequency(10001, TimeUnit.MILLISECONDS);
			settings.minHeartbeatFrequency(501, TimeUnit.MILLISECONDS);
		}).applyToConnectionPoolSettings((settings) -> {
			settings.maxWaitTime(120001, TimeUnit.MILLISECONDS);
			settings.maxConnectionLifeTime(60000, TimeUnit.MILLISECONDS);
			settings.maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS);
		}).applyToSslSettings((settings) -> settings.enabled(true)).applicationName("test");

		MongoClientSettings settings = builder.build();
		T client = createMongoClient(settings);
		MongoClientSettings wrapped = getClientSettings(client);
		assertThat(wrapped.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getServerSettings().getHeartbeatFrequency(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getServerSettings().getHeartbeatFrequency(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getServerSettings().getMinHeartbeatFrequency(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getServerSettings().getMinHeartbeatFrequency(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getApplicationName()).isEqualTo(settings.getApplicationName());
		assertThat(wrapped.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getSslSettings().isEnabled()).isEqualTo(settings.getSslSettings().isEnabled());
	}

	@Test
	void customizerIsInvoked() {
		MongoClientSettingsBuilderCustomizer customizer = mock(MongoClientSettingsBuilderCustomizer.class);
		createMongoClient(customizer);
		verify(customizer).customize(any(MongoClientSettings.Builder.class));
	}

	@Test
	void canBindAutoIndexCreation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.autoIndexCreation:true").applyTo(context);
		context.register(Config.class);
		context.refresh();
		MongoProperties properties = context.getBean(MongoProperties.class);
		assertThat(properties.isAutoIndexCreation()).isTrue();
	}

	protected T createMongoClient() {
		return createMongoClient(null, MongoClientSettings.builder().build());
	}

	protected T createMongoClient(MongoClientSettings settings) {
		return createMongoClient(null, settings);
	}

	protected void createMongoClient(MongoClientSettingsBuilderCustomizer... customizers) {
		createMongoClient((customizers != null) ? Arrays.asList(customizers) : null,
				MongoClientSettings.builder().build());
	}

	protected abstract T createMongoClient(List<MongoClientSettingsBuilderCustomizer> customizers,
			MongoClientSettings settings);

	protected abstract MongoClientSettings getClientSettings(T client);

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MongoProperties.class)
	static class Config {

	}

}
