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

import java.util.List;
import java.util.stream.Collectors;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.event.CommandListener;
import com.mongodb.event.ConnectionPoolListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoMetricsCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoMetricsConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolTagsProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mongo.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author Jonatan Ivanov
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MongoClient.class)
@EnableConfigurationProperties(MongoProperties.class)
@ConditionalOnMissingBean(type = "org.springframework.data.mongodb.MongoDatabaseFactory")
public class MongoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MongoClient.class)
	public MongoClient mongo(ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
			MongoClientSettings settings) {
		return new MongoClientFactory(builderCustomizers.orderedStream().collect(Collectors.toList()))
				.createMongoClient(settings);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(MongoClientSettings.class)
	static class MongoClientSettingsConfiguration {

		@Bean
		MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().build();
		}

		@Bean
		MongoPropertiesClientSettingsBuilderCustomizer mongoPropertiesCustomizer(MongoProperties properties,
				Environment environment) {
			return new MongoPropertiesClientSettingsBuilderCustomizer(properties, environment);
		}

		@Bean
		@ConditionalOnBean(CommandListener.class)
		MongoCommandListenerClientSettingsBuilderCustomizer mongoCommandListenerClientSettingsBuilderCustomizer(
				List<CommandListener> commandListeners) {
			return new MongoCommandListenerClientSettingsBuilderCustomizer(commandListeners);
		}

		@Bean
		@ConditionalOnBean(ConnectionPoolListener.class)
		MongoConnectionPoolListenerClientSettingsBuilderCustomizer mongoConnectionPoolListenerClientSettingsBuilderCustomizer(
				List<ConnectionPoolListener> connectionPoolListeners) {
			return new MongoConnectionPoolListenerClientSettingsBuilderCustomizer(connectionPoolListeners);
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnBean(MeterRegistry.class)
		// TODO: Add to MongoProperties?
		@ConditionalOnProperty(value = "spring.data.mongodb.metrics.enabled", matchIfMissing = true)
		static class MongoMetricsConfiguration {

			@Bean
			@ConditionalOnMissingBean
			MongoMetricsCommandListener mongoMetricsCommandListener(MeterRegistry meterRegistry,
					MongoMetricsCommandTagsProvider mongoMetricsCommandTagsProvider) {
				return new MongoMetricsCommandListener(meterRegistry, mongoMetricsCommandTagsProvider);
			}

			@Bean
			@ConditionalOnMissingBean
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

}
