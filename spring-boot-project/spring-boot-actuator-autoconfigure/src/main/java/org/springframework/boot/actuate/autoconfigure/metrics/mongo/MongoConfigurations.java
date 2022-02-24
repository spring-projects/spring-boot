/*
 * Copyright 2012-2022 the original author or authors.
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

import io.micrometer.binder.mongodb.DefaultMongoCommandTagsProvider;
import io.micrometer.binder.mongodb.DefaultMongoConnectionPoolTagsProvider;
import io.micrometer.binder.mongodb.MongoCommandTagsProvider;
import io.micrometer.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configurations, imported from {@link MongoMetricsAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
abstract class MongoConfigurations {

	@Configuration(proxyBeanMethods = false)
	static class MongoCommandTagsProviderConfiguration {

		@Bean
		@ConditionalOnMissingBean({ MongoCommandTagsProvider.class,
				io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider.class })
		MongoCommandTagsProvider mongoCommandTagsProvider() {
			return new DefaultMongoCommandTagsProvider();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(MongoCommandTagsProviderConfiguration.class)
	static class MongoMetricsCommandListenerConfiguration {

		@Bean
		@ConditionalOnMissingBean({ MongoMetricsCommandListener.class,
				io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener.class })
		@ConditionalOnBean(MongoCommandTagsProvider.class)
		MongoMetricsCommandListener mongoMetricsCommandListener(MeterRegistry meterRegistry,
				MongoCommandTagsProvider mongoCommandTagsProvider) {
			return new MongoMetricsCommandListener(meterRegistry, mongoCommandTagsProvider);
		}

		@Bean
		@ConditionalOnMissingBean({ MongoMetricsCommandListener.class,
				io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener.class })
		@ConditionalOnBean(io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider.class)
		io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener mongoMetricsCommandListenerBackwardsCompatible(
				MeterRegistry meterRegistry,
				io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider mongoCommandTagsProvider) {
			return new io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener(meterRegistry,
					mongoCommandTagsProvider);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MongoConnectionPoolTagsProviderConfiguration {

		@Bean
		@ConditionalOnMissingBean({ io.micrometer.binder.mongodb.MongoConnectionPoolTagsProvider.class,
				io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider.class })
		io.micrometer.binder.mongodb.MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider() {
			return new DefaultMongoConnectionPoolTagsProvider();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(MongoConnectionPoolTagsProviderConfiguration.class)
	static class MongoMetricsConnectionPoolListenerConfiguration {

		@Bean
		@ConditionalOnMissingBean({ io.micrometer.binder.mongodb.MongoMetricsConnectionPoolListener.class,
				io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener.class })
		@ConditionalOnBean(io.micrometer.binder.mongodb.MongoConnectionPoolTagsProvider.class)
		io.micrometer.binder.mongodb.MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListener(
				MeterRegistry meterRegistry,
				io.micrometer.binder.mongodb.MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider) {
			return new io.micrometer.binder.mongodb.MongoMetricsConnectionPoolListener(meterRegistry,
					mongoConnectionPoolTagsProvider);
		}

		@Bean
		@ConditionalOnMissingBean({ io.micrometer.binder.mongodb.MongoMetricsConnectionPoolListener.class,
				io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener.class })
		@ConditionalOnBean(io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider.class)
		io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListenerBackwardsCompatible(
				MeterRegistry meterRegistry,
				io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider) {
			return new io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener(meterRegistry,
					mongoConnectionPoolTagsProvider);
		}

	}

}
