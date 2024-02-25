/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.jdbc.DataSourcePoolMetrics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceUnwrapper;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link DataSource datasources}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, DataSourceAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnClass({ DataSource.class, MeterRegistry.class })
@ConditionalOnBean({ DataSource.class, MeterRegistry.class })
public class DataSourcePoolMetricsAutoConfiguration {

	/**
	 * DataSourcePoolMetadataMetricsConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(DataSourcePoolMetadataProvider.class)
	static class DataSourcePoolMetadataMetricsConfiguration {

		private static final String DATASOURCE_SUFFIX = "dataSource";

		/**
		 * Creates a new instance of DataSourcePoolMetadataMeterBinder.
		 * @param dataSources the map of data sources
		 * @param metadataProviders the provider for data source pool metadata
		 * @return the DataSourcePoolMetadataMeterBinder instance
		 */
		@Bean
		DataSourcePoolMetadataMeterBinder dataSourcePoolMetadataMeterBinder(Map<String, DataSource> dataSources,
				ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders) {
			return new DataSourcePoolMetadataMeterBinder(dataSources, metadataProviders);
		}

		/**
		 * DataSourcePoolMetadataMeterBinder class.
		 */
		static class DataSourcePoolMetadataMeterBinder implements MeterBinder {

			private final Map<String, DataSource> dataSources;

			private final ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders;

			/**
			 * Constructs a new DataSourcePoolMetadataMeterBinder with the specified
			 * dataSources and metadataProviders.
			 * @param dataSources the map of data sources, where the key is the data
			 * source name and the value is the data source object
			 * @param metadataProviders the object provider for the data source pool
			 * metadata providers
			 */
			DataSourcePoolMetadataMeterBinder(Map<String, DataSource> dataSources,
					ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders) {
				this.dataSources = dataSources;
				this.metadataProviders = metadataProviders;
			}

			/**
			 * Binds the DataSourcePoolMetadataProvider to the given MeterRegistry.
			 * @param registry the MeterRegistry to bind to
			 */
			@Override
			public void bindTo(MeterRegistry registry) {
				List<DataSourcePoolMetadataProvider> metadataProvidersList = this.metadataProviders.stream().toList();
				this.dataSources.forEach((name, dataSource) -> bindDataSourceToRegistry(name, dataSource,
						metadataProvidersList, registry));
			}

			/**
			 * Binds the given {@link DataSource} to the provided {@link MeterRegistry} by
			 * creating and registering a new {@link DataSourcePoolMetrics} instance.
			 * @param beanName the name of the bean associated with the {@link DataSource}
			 * @param dataSource the {@link DataSource} to bind to the
			 * {@link MeterRegistry}
			 * @param metadataProviders the collection of
			 * {@link DataSourcePoolMetadataProvider}s
			 * @param registry the {@link MeterRegistry} to bind the {@link DataSource} to
			 */
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

		}

	}

	/**
	 * HikariDataSourceMetricsConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	static class HikariDataSourceMetricsConfiguration {

		/**
		 * Creates a HikariDataSourceMeterBinder object for monitoring Hikari data
		 * sources.
		 * @param dataSources the provider for obtaining the data sources
		 * @return a HikariDataSourceMeterBinder object
		 */
		@Bean
		HikariDataSourceMeterBinder hikariDataSourceMeterBinder(ObjectProvider<DataSource> dataSources) {
			return new HikariDataSourceMeterBinder(dataSources);
		}

		/**
		 * HikariDataSourceMeterBinder class.
		 */
		static class HikariDataSourceMeterBinder implements MeterBinder {

			private static final Log logger = LogFactory.getLog(HikariDataSourceMeterBinder.class);

			private final ObjectProvider<DataSource> dataSources;

			/**
			 * Constructs a new HikariDataSourceMeterBinder with the specified
			 * dataSources.
			 * @param dataSources the ObjectProvider of DataSource objects to be used by
			 * the meter binder
			 */
			HikariDataSourceMeterBinder(ObjectProvider<DataSource> dataSources) {
				this.dataSources = dataSources;
			}

			/**
			 * Binds the given MeterRegistry to the HikariDataSource instances in the
			 * dataSources list.
			 * @param registry the MeterRegistry to bind
			 */
			@Override
			public void bindTo(MeterRegistry registry) {
				for (DataSource dataSource : this.dataSources) {
					HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class,
							HikariDataSource.class);
					if (hikariDataSource != null) {
						bindMetricsRegistryToHikariDataSource(hikariDataSource, registry);
					}
				}
			}

			/**
			 * Binds the given MeterRegistry to the HikariDataSource if the
			 * HikariDataSource does not already have a MetricRegistry or
			 * MetricsTrackerFactory set.
			 * @param hikari - The HikariDataSource to bind the MeterRegistry to.
			 * @param registry - The MeterRegistry to bind to the HikariDataSource.
			 */
			private void bindMetricsRegistryToHikariDataSource(HikariDataSource hikari, MeterRegistry registry) {
				if (hikari.getMetricRegistry() == null && hikari.getMetricsTrackerFactory() == null) {
					try {
						hikari.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));
					}
					catch (Exception ex) {
						logger.warn(LogMessage.format("Failed to bind Hikari metrics: %s", ex.getMessage()));
					}
				}
			}

		}

	}

}
