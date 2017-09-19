/*
 * Copyright 2012-2017 the original author or authors.
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

import com.couchbase.client.java.Bucket;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.couchbase.CouchbaseHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.couchbase.CouchbaseDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.core.CouchbaseOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CouchbaseHealthIndicator}.
 *
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ CouchbaseOperations.class, Bucket.class })
@ConditionalOnBean(CouchbaseOperations.class)
@ConditionalOnEnabledHealthIndicator("couchbase")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@AutoConfigureAfter(CouchbaseDataAutoConfiguration.class)
public class CouchbaseHealthIndicatorAutoConfiguration extends
		CompositeHealthIndicatorConfiguration<CouchbaseHealthIndicator, CouchbaseOperations> {

	private final Map<String, CouchbaseOperations> couchbaseOperations;

	public CouchbaseHealthIndicatorAutoConfiguration(
			Map<String, CouchbaseOperations> couchbaseOperations) {
		this.couchbaseOperations = couchbaseOperations;
	}

	@Bean
	@ConditionalOnMissingBean(name = "couchbaseHealthIndicator")
	public HealthIndicator couchbaseHealthIndicator() {
		return createHealthIndicator(this.couchbaseOperations);
	}

}
