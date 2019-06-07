/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cassandra;

import java.util.Map;

import com.datastax.driver.core.Cluster;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.cassandra.CassandraHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CassandraHealthIndicator}.
 *
 * @author Julien Dubois
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ CassandraOperations.class, Cluster.class })
@ConditionalOnBean(CassandraOperations.class)
@ConditionalOnEnabledHealthIndicator("cassandra")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@AutoConfigureAfter({ CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class })
public class CassandraHealthIndicatorAutoConfiguration
		extends CompositeHealthIndicatorConfiguration<CassandraHealthIndicator, CassandraOperations> {

	private final Map<String, CassandraOperations> cassandraOperations;

	public CassandraHealthIndicatorAutoConfiguration(Map<String, CassandraOperations> cassandraOperations) {
		this.cassandraOperations = cassandraOperations;
	}

	@Bean
	@ConditionalOnMissingBean(name = "cassandraHealthIndicator")
	public HealthIndicator cassandraHealthIndicator() {
		return createHealthIndicator(this.cassandraOperations);
	}

}
