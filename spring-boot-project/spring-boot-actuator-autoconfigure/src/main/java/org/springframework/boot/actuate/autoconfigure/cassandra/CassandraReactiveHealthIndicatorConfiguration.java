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
package org.springframework.boot.actuate.autoconfigure.cassandra;

import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthIndicatorConfiguration;
import org.springframework.boot.actuate.cassandra.CassandraReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;

/**
 * Configuration for {@link CassandraReactiveHealthIndicator}.
 *
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass(ReactiveCassandraOperations.class)
@ConditionalOnBean(ReactiveCassandraOperations.class)
class CassandraReactiveHealthIndicatorConfiguration extends
		CompositeReactiveHealthIndicatorConfiguration<CassandraReactiveHealthIndicator, ReactiveCassandraOperations> {

	private final Map<String, ReactiveCassandraOperations> reactiveCassandraOperations;

	CassandraReactiveHealthIndicatorConfiguration(
			Map<String, ReactiveCassandraOperations> reactiveCassandraOperations) {
		this.reactiveCassandraOperations = reactiveCassandraOperations;
	}

	@Bean
	@ConditionalOnMissingBean(name = "cassandraReactiveHealthIndicator")
	public ReactiveHealthIndicator cassandraHealthIndicator() {
		return createHealthIndicator(this.reactiveCassandraOperations);
	}

}
