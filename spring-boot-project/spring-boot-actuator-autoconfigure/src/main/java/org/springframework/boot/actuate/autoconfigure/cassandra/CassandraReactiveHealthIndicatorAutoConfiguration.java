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
package org.springframework.boot.actuate.autoconfigure.cassandra;

import com.datastax.driver.core.Cluster;
import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.cassandra.CassandraReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;

import java.util.Map;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link org.springframework.boot.actuate.cassandra.CassandraReactiveHealthIndicator}.
 *
 * @author Artsiom Yudovin
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ReactiveCassandraOperations.class, Cluster.class })
@ConditionalOnBean(ReactiveCassandraOperations.class)
@ConditionalOnEnabledHealthIndicator("cassandra")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@AutoConfigureAfter({ CassandraAutoConfiguration.class,
		CassandraReactiveDataAutoConfiguration.class })
public class CassandraReactiveHealthIndicatorAutoConfiguration extends
		CompositeReactiveHealthIndicatorConfiguration<CassandraReactiveHealthIndicator, ReactiveCassandraOperations> {
	private final Map<String, ReactiveCassandraOperations> reactiveCassandraOperations;

	public CassandraReactiveHealthIndicatorAutoConfiguration(
			Map<String, ReactiveCassandraOperations> reactiveCassandraOperations) {
		this.reactiveCassandraOperations = reactiveCassandraOperations;
	}

	@Bean
	@ConditionalOnMissingBean(name = "cassandraReactiveHealthIndicator")
	public ReactiveHealthIndicator cassandraHealthIndicator() {
		return createHealthIndicator(this.reactiveCassandraOperations);
	}
}
