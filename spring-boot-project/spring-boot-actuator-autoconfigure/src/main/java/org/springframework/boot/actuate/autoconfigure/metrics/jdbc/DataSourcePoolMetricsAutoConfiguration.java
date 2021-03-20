/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.jdbc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.jdbc.DataSourcePoolMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceUnwrapper;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogMessage;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link DataSource datasources}.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, DataSourceAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnClass({ DataSource.class, MeterRegistry.class })
@ConditionalOnBean({ DataSource.class, MeterRegistry.class })
@EnableConfigurationProperties(DataSourcePoolMetricsProperties.class)
public class DataSourcePoolMetricsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(DataSourcePoolMetadataProvider.class)
	static class DataSourcePoolMetadataMetricsConfiguration {

		private static final String DATASOURCE_SUFFIX = "dataSource";

		@Autowired
		void bindDataSourcesToRegistry(Map<String, DataSource> dataSources, MeterRegistry registry,
				ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders,
				DataSourcePoolMetricsProperties properties) {
			List<DataSourcePoolMetadataProvider> metadataProvidersList = metadataProviders.stream()
					.collect(Collectors.toList());

			// Possibly expand routing datasources into their resolved datasources
			Map<String, DataSource> allDataSources = new HashMap<>();
			dataSources.forEach((beanName, dataSource) -> {
				if (!(dataSource instanceof AbstractRoutingDataSource)) {
					allDataSources.put(beanName, dataSource);
					return;
				}
				if (properties.isIgnoreRoutingDataSources()) {
					return;
				}
				AbstractRoutingDataSource routingDataSource = (AbstractRoutingDataSource) dataSource;
				routingDataSource.getResolvedDataSources().forEach((routingKey, routedDataSource) -> {
					boolean alreadyRegistered = dataSources.containsValue(routedDataSource);
					if (!properties.isDeduplicateRoutingDataSources() || !alreadyRegistered) {
						allDataSources.put(getRoutedDataSourceName(beanName, routingKey), routedDataSource);
					}
				});
			});

			allDataSources.forEach(
					(name, dataSource) -> bindDataSourceToRegistry(name, dataSource, metadataProvidersList, registry));
		}

		private void bindDataSourceToRegistry(String beanName, DataSource dataSource,
				Collection<DataSourcePoolMetadataProvider> metadataProviders, MeterRegistry registry) {
			String dataSourceName = getDataSourceName(beanName);
			new DataSourcePoolMetrics(dataSource, metadataProviders, dataSourceName, Collections.emptyList())
					.bindTo(registry);
		}

		/**
		 * Get the name of a DataSource based on its {@code beanName}.
		 * @param beanName the name of the data source bean
		 * @return a name for the given data source
		 */
		private String getDataSourceName(String beanName) {
			if (beanName.length() > DATASOURCE_SUFFIX.length()
					&& StringUtils.endsWithIgnoreCase(beanName, DATASOURCE_SUFFIX)) {
				return beanName.substring(0, beanName.length() - DATASOURCE_SUFFIX.length());
			}
			return beanName;
		}

		/**
		 * Gets the name of a routed DataSource based on the name of its containing
		 * {@link AbstractRoutingDataSource} {@code beanName} and its {@code routingKey}.
		 * {@link #getDataSourceName}
		 * @param routingDataSourceBeanName the name of the routing data source bean
		 * @param routingKey the routing key for the routed data source
		 * @return a name for the given routed data source
		 */
		private String getRoutedDataSourceName(String routingDataSourceBeanName, Object routingKey) {
			return getDataSourceName(routingDataSourceBeanName) + "." + routingKey;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	static class HikariDataSourceMetricsConfiguration {

		private static final Log logger = LogFactory.getLog(HikariDataSourceMetricsConfiguration.class);

		private final MeterRegistry registry;

		HikariDataSourceMetricsConfiguration(MeterRegistry registry) {
			this.registry = registry;
		}

		@Autowired
		void bindMetricsRegistryToHikariDataSources(Collection<DataSource> dataSources) {
			for (DataSource dataSource : dataSources) {
				HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class,
						HikariDataSource.class);
				if (hikariDataSource != null) {
					bindMetricsRegistryToHikariDataSource(hikariDataSource);
				}
			}
		}

		private void bindMetricsRegistryToHikariDataSource(HikariDataSource hikari) {
			if (hikari.getMetricRegistry() == null && hikari.getMetricsTrackerFactory() == null) {
				try {
					hikari.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(this.registry));
				}
				catch (Exception ex) {
					logger.warn(LogMessage.format("Failed to bind Hikari metrics: %s", ex.getMessage()));
				}
			}
		}

	}

}
