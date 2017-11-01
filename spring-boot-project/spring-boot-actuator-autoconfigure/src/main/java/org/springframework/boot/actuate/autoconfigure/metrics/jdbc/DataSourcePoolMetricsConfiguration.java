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

package org.springframework.boot.actuate.autoconfigure.metrics.jdbc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.jdbc.DataSourcePoolMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Configuration;

/**
 * Configure metrics for all available {@link DataSource datasources}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnBean({ DataSource.class, DataSourcePoolMetadataProvider.class })
@ConditionalOnProperty(value = "spring.metrics.jdbc.instrument-datasource", matchIfMissing = true)
@EnableConfigurationProperties(JdbcMetricsProperties.class)
public class DataSourcePoolMetricsConfiguration {

	private static final String DATASOURCE_SUFFIX = "dataSource";

	private final MeterRegistry registry;

	private final Collection<DataSourcePoolMetadataProvider> metadataProviders;

	private final String metricName;

	public DataSourcePoolMetricsConfiguration(MeterRegistry registry,
			Collection<DataSourcePoolMetadataProvider> metadataProviders,
			JdbcMetricsProperties jdbcMetricsProperties) {
		this.registry = registry;
		this.metadataProviders = metadataProviders;
		this.metricName = jdbcMetricsProperties.getDatasourceMetricName();
	}

	@Autowired
	public void bindDataSourcesToRegistry(Map<String, DataSource> dataSources) {
		dataSources.forEach(this::bindDataSourceToRegistry);
	}

	private void bindDataSourceToRegistry(String beanName, DataSource dataSource) {
		List<Tag> tags = Tags.zip("name", getDataSourceName(beanName));
		new DataSourcePoolMetrics(dataSource, this.metadataProviders, this.metricName,
				tags).bindTo(this.registry);
	}

	/**
	 * Get the name of a DataSource based on its {@code beanName}.
	 * @param beanName the name of the data source bean
	 * @return a name for the given data source
	 */
	private String getDataSourceName(String beanName) {
		if (beanName.length() > DATASOURCE_SUFFIX.length()
				&& beanName.toLowerCase().endsWith(DATASOURCE_SUFFIX.toLowerCase())) {
			return beanName.substring(0, beanName.length() - DATASOURCE_SUFFIX.length());
		}
		return beanName;
	}

}
