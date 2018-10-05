/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.actuate.autoconfigure.couchbase;

import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthIndicatorConfiguration;
import org.springframework.boot.actuate.couchbase.CouchbaseReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.core.RxJavaCouchbaseOperations;

/**
 * Configuration for {@link CouchbaseReactiveHealthIndicator}.
 *
 * @author Mikalai Lushchytski
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Configuration
@ConditionalOnClass({ RxJavaCouchbaseOperations.class, Flux.class })
@ConditionalOnBean(RxJavaCouchbaseOperations.class)
@EnableConfigurationProperties(CouchbaseHealthIndicatorProperties.class)
public class CouchbaseReactiveHealthIndicatorConfiguration extends
		CompositeReactiveHealthIndicatorConfiguration<CouchbaseReactiveHealthIndicator, RxJavaCouchbaseOperations> {

	private final Map<String, RxJavaCouchbaseOperations> couchbaseOperations;

	private final CouchbaseHealthIndicatorProperties properties;

	CouchbaseReactiveHealthIndicatorConfiguration(
			Map<String, RxJavaCouchbaseOperations> couchbaseOperations,
			CouchbaseHealthIndicatorProperties properties) {
		this.couchbaseOperations = couchbaseOperations;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(name = "couchbaseReactiveHealthIndicator")
	public ReactiveHealthIndicator couchbaseReactiveHealthIndicator() {
		return createHealthIndicator(this.couchbaseOperations);
	}

	@Override
	protected CouchbaseReactiveHealthIndicator createHealthIndicator(
			RxJavaCouchbaseOperations couchbaseOperations) {
		return new CouchbaseReactiveHealthIndicator(couchbaseOperations,
				this.properties.getTimeout());
	}

}
