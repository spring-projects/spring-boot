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

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.cassandra.CassandraHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraOperations;

/**
 * Configuration for {@link CassandraHealthIndicator}.
 *
 * @author Julien Dubois
 */
@Configuration
@ConditionalOnClass(CassandraOperations.class)
@ConditionalOnBean(CassandraOperations.class)
class CassandraHealthIndicatorConfiguration extends
		CompositeHealthIndicatorConfiguration<CassandraHealthIndicator, CassandraOperations> {

	private final Map<String, CassandraOperations> cassandraOperations;

	CassandraHealthIndicatorConfiguration(
			Map<String, CassandraOperations> cassandraOperations) {
		this.cassandraOperations = cassandraOperations;
	}

	@Bean
	@ConditionalOnMissingBean(name = "cassandraHealthIndicator")
	public HealthIndicator cassandraHealthIndicator() {
		return createHealthIndicator(this.cassandraOperations);
	}

}
