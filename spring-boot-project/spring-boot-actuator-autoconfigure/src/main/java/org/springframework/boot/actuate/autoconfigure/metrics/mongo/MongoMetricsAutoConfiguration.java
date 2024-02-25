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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mongo metrics.
 *
 * @author Chris Bono
 * @author Jonatan Ivanov
 * @since 2.5.0
 */
@AutoConfiguration(before = MongoAutoConfiguration.class,
		after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass(MongoClientSettings.class)
@ConditionalOnBean(MeterRegistry.class)
public class MongoMetricsAutoConfiguration {

	/**
	 * MongoCommandMetricsConfiguration class.
	 */
	@ConditionalOnClass(MongoMetricsCommandListener.class)
	@ConditionalOnProperty(name = "management.metrics.mongo.command.enabled", havingValue = "true",
			matchIfMissing = true)
	static class MongoCommandMetricsConfiguration {

		/**
		 * Creates a new instance of {@link MongoMetricsCommandListener} if no other bean
		 * of the same type is present in the application context.
		 * @param meterRegistry the {@link MeterRegistry} used for collecting and
		 * reporting metrics
		 * @param mongoCommandTagsProvider the {@link MongoCommandTagsProvider} used for
		 * providing tags for MongoDB commands
		 * @return a new instance of {@link MongoMetricsCommandListener}
		 */
		@Bean
		@ConditionalOnMissingBean
		MongoMetricsCommandListener mongoMetricsCommandListener(MeterRegistry meterRegistry,
				MongoCommandTagsProvider mongoCommandTagsProvider) {
			return new MongoMetricsCommandListener(meterRegistry, mongoCommandTagsProvider);
		}

		/**
		 * Creates a new instance of {@link MongoCommandTagsProvider} if no other bean of
		 * the same type is present in the application context.
		 * @return the {@link MongoCommandTagsProvider} bean
		 */
		@Bean
		@ConditionalOnMissingBean
		MongoCommandTagsProvider mongoCommandTagsProvider() {
			return new DefaultMongoCommandTagsProvider();
		}

		/**
		 * Returns a customizer for the MongoClientSettingsBuilder that adds a command
		 * listener for MongoDB metrics.
		 * @param mongoMetricsCommandListener the command listener for MongoDB metrics
		 * @return the customizer for the MongoClientSettingsBuilder
		 */
		@Bean
		MongoClientSettingsBuilderCustomizer mongoMetricsCommandListenerClientSettingsBuilderCustomizer(
				MongoMetricsCommandListener mongoMetricsCommandListener) {
			return (clientSettingsBuilder) -> clientSettingsBuilder.addCommandListener(mongoMetricsCommandListener);
		}

	}

	/**
	 * MongoConnectionPoolMetricsConfiguration class.
	 */
	@ConditionalOnClass(MongoMetricsConnectionPoolListener.class)
	@ConditionalOnProperty(name = "management.metrics.mongo.connectionpool.enabled", havingValue = "true",
			matchIfMissing = true)
	static class MongoConnectionPoolMetricsConfiguration {

		/**
		 * Creates a new instance of {@link MongoMetricsConnectionPoolListener} if no
		 * other bean of the same type is present. This listener is responsible for
		 * collecting metrics related to the MongoDB connection pool.
		 * @param meterRegistry the {@link MeterRegistry} used to register the metrics
		 * @param mongoConnectionPoolTagsProvider the
		 * {@link MongoConnectionPoolTagsProvider} used to provide tags for the metrics
		 * @return the {@link MongoMetricsConnectionPoolListener} instance
		 */
		@Bean
		@ConditionalOnMissingBean
		MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListener(MeterRegistry meterRegistry,
				MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider) {
			return new MongoMetricsConnectionPoolListener(meterRegistry, mongoConnectionPoolTagsProvider);
		}

		/**
		 * Creates a new instance of {@link MongoConnectionPoolTagsProvider} if no other
		 * bean of the same type is present in the application context.
		 * @return the {@link MongoConnectionPoolTagsProvider} bean
		 */
		@Bean
		@ConditionalOnMissingBean
		MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider() {
			return new DefaultMongoConnectionPoolTagsProvider();
		}

		/**
		 * Returns a customizer for MongoClientSettingsBuilder that adds a
		 * MongoMetricsConnectionPoolListener to the connection pool settings.
		 * @param mongoMetricsConnectionPoolListener the
		 * MongoMetricsConnectionPoolListener to be added to the connection pool settings
		 * @return the customizer for MongoClientSettingsBuilder
		 */
		@Bean
		MongoClientSettingsBuilderCustomizer mongoMetricsConnectionPoolListenerClientSettingsBuilderCustomizer(
				MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListener) {
			return (clientSettingsBuilder) -> clientSettingsBuilder
				.applyToConnectionPoolSettings((connectionPoolSettingsBuilder) -> connectionPoolSettingsBuilder
					.addConnectionPoolListener(mongoMetricsConnectionPoolListener));
		}

	}

}
