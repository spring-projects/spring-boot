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

import com.mongodb.MongoClientSettings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mongo metrics.
 *
 * @author Chris Bono
 * @author Jonatan Ivanov
 * @since 2.5.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(MongoAutoConfiguration.class)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass(MongoClientSettings.class)
@ConditionalOnBean(MeterRegistry.class)
public class MongoMetricsAutoConfiguration {

	@ConditionalOnClass(MongoMetricsCommandListener.class)
	@ConditionalOnProperty(name = "management.metrics.mongo.command.enabled", havingValue = "true",
			matchIfMissing = true)
	static class MongoCommandMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		MongoMetricsCommandListener mongoMetricsCommandListener(MeterRegistry meterRegistry,
				MongoCommandTagsProvider mongoCommandTagsProvider) {
			return new MongoMetricsCommandListener(meterRegistry, mongoCommandTagsProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		MongoCommandTagsProvider mongoCommandTagsProvider() {
			return new DefaultMongoCommandTagsProvider();
		}

		@Bean
		MongoClientSettingsBuilderCustomizer mongoMetricsCommandListenerClientSettingsBuilderCustomizer(
				MongoMetricsCommandListener mongoMetricsCommandListener) {
			return (clientSettingsBuilder) -> clientSettingsBuilder.addCommandListener(mongoMetricsCommandListener);
		}

	}

	@ConditionalOnClass(MongoMetricsConnectionPoolListener.class)
	@ConditionalOnProperty(name = "management.metrics.mongo.connectionpool.enabled", havingValue = "true",
			matchIfMissing = true)
	static class MongoConnectionPoolMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListener(MeterRegistry meterRegistry,
				MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider) {
			return new MongoMetricsConnectionPoolListener(meterRegistry, mongoConnectionPoolTagsProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider() {
			return new DefaultMongoConnectionPoolTagsProvider();
		}

		@Bean
		MongoClientSettingsBuilderCustomizer mongoMetricsConnectionPoolListenerClientSettingsBuilderCustomizer(
				MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListener) {
			return (clientSettingsBuilder) -> clientSettingsBuilder
					.applyToConnectionPoolSettings((connectionPoolSettingsBuilder) -> connectionPoolSettingsBuilder
							.addConnectionPoolListener(mongoMetricsConnectionPoolListener));
		}

	}

}
