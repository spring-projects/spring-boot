/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.boot.actuate.autoconfigure.metrics.cassandra;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.metrics.DefaultNodeMetric;
import com.datastax.oss.driver.api.core.metrics.DefaultSessionMetric;
import com.datastax.oss.driver.internal.metrics.micrometer.MicrometerMetricsFactory;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.autoconfigure.cassandra.DriverConfigLoaderBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link CqlSession Cassandra sessions}.
 *
 * @author Erik Merkle
 * @since 2.4.0
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureBefore({ CassandraAutoConfiguration.class })
@ConditionalOnClass({ MeterRegistry.class, MicrometerMetricsFactory.class })
@ConditionalOnBean({ MeterRegistry.class })
public class CassandraMetricsAutoConfiguration {

	@Bean
	public DriverConfigLoaderBuilderCustomizer cassandraMetricsConfigCustomizer() {
		return (builder) -> builder.withString(DefaultDriverOption.METRICS_FACTORY_CLASS, "MicrometerMetricsFactory")
				.withStringList(DefaultDriverOption.METRICS_SESSION_ENABLED,
						Stream.of(DefaultSessionMetric.values()).map(DefaultSessionMetric::getPath)
								.collect(Collectors.toList()))
				.withStringList(DefaultDriverOption.METRICS_NODE_ENABLED, Stream.of(DefaultNodeMetric.values())
						.map(DefaultNodeMetric::getPath).collect(Collectors.toList()));
	}

	@Bean
	public CqlSessionBuilderCustomizer cassandraMetricsBuilderCustomizer(MeterRegistry registry) {
		return (builder) -> builder.withMetricRegistry(registry);
	}

}
