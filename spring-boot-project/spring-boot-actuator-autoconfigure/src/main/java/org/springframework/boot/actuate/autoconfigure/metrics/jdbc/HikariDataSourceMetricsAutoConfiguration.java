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

package org.springframework.boot.actuate.autoconfigure.metrics.jdbc;

import java.util.Collection;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all
 * {@link HikariDataSource HikariDataSources}.
 *
 * @author Tommy Ludwig
 * @since 2.0.0
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class, DataSourceAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnClass(HikariDataSource.class)
@ConditionalOnBean({ DataSource.class, MeterRegistry.class })
public class HikariDataSourceMetricsAutoConfiguration {

	private final MeterRegistry registry;

	public HikariDataSourceMetricsAutoConfiguration(MeterRegistry registry) {
		this.registry = registry;
	}

	@Autowired
	public void bindMetricsRegistryToHikariDataSources(
			Collection<DataSource> dataSources) {
		dataSources.stream().filter(HikariDataSource.class::isInstance)
				.map(HikariDataSource.class::cast)
				.forEach(this::bindMetricsRegistryToHikariDataSource);
	}

	private void bindMetricsRegistryToHikariDataSource(HikariDataSource hikari) {
		hikari.setMetricsTrackerFactory(
				new MicrometerMetricsTrackerFactory(this.registry));
	}

}
