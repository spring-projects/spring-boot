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

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.couchbase.CouchbaseHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.core.CouchbaseOperations;

/**
 * Configuration for {@link CouchbaseHealthIndicator}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(CouchbaseOperations.class)
@ConditionalOnBean(CouchbaseOperations.class)
@EnableConfigurationProperties(CouchbaseHealthIndicatorProperties.class)
public class CouchbaseHealthIndicatorConfiguration extends
		CompositeHealthIndicatorConfiguration<CouchbaseHealthIndicator, CouchbaseOperations> {

	private final Map<String, CouchbaseOperations> couchbaseOperations;

	private final CouchbaseHealthIndicatorProperties properties;

	CouchbaseHealthIndicatorConfiguration(
			Map<String, CouchbaseOperations> couchbaseOperations,
			CouchbaseHealthIndicatorProperties properties) {
		this.couchbaseOperations = couchbaseOperations;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(name = "couchbaseHealthIndicator")
	public HealthIndicator couchbaseHealthIndicator() {
		return createHealthIndicator(this.couchbaseOperations);
	}

	@Override
	protected CouchbaseHealthIndicator createHealthIndicator(
			CouchbaseOperations couchbaseOperations) {
		return new CouchbaseHealthIndicator(couchbaseOperations,
				this.properties.getTimeout());
	}

}
