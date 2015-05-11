/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import javax.servlet.Servlet;
import javax.sql.DataSource;

import org.apache.catalina.startup.Tomcat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.cache.CacheStatisticsProvider;
import org.springframework.boot.actuate.endpoint.CachePublicMetrics;
import org.springframework.boot.actuate.endpoint.DataSourcePublicMetrics;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RichGaugeReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.SystemPublicMetrics;
import org.springframework.boot.actuate.endpoint.TomcatPublicMetrics;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGaugeReader;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link PublicMetrics}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Johannes Stelzer
 * @since 1.2.0
 */
@Configuration
@AutoConfigureBefore(EndpointAutoConfiguration.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, CacheAutoConfiguration.class,
		MetricRepositoryAutoConfiguration.class, CacheStatisticsAutoConfiguration.class })
public class PublicMetricsAutoConfiguration {

	@Autowired(required = false)
	@ActuatorMetricRepository
	private MetricReader metricReader = new InMemoryMetricRepository();

	@Bean
	public SystemPublicMetrics systemPublicMetrics() {
		return new SystemPublicMetrics();
	}

	@Bean
	public MetricReaderPublicMetrics metricReaderPublicMetrics() {
		return new MetricReaderPublicMetrics(this.metricReader);
	}

	@Bean
	@ConditionalOnBean(RichGaugeReader.class)
	public RichGaugeReaderPublicMetrics richGaugePublicMetrics(
			RichGaugeReader richGaugeReader) {
		return new RichGaugeReaderPublicMetrics(richGaugeReader);
	}

	@Configuration
	@ConditionalOnClass(DataSource.class)
	@ConditionalOnBean(DataSource.class)
	static class DataSourceMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(DataSourcePoolMetadataProvider.class)
		public DataSourcePublicMetrics dataSourcePublicMetrics() {
			return new DataSourcePublicMetrics();
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class, Tomcat.class })
	static class TomcatMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public TomcatPublicMetrics tomcatPublicMetrics() {
			return new TomcatPublicMetrics();
		}

	}

	@Configuration
	@ConditionalOnClass(CacheManager.class)
	@ConditionalOnBean(CacheManager.class)
	static class CacheStatisticsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(CacheStatisticsProvider.class)
		public CachePublicMetrics cachePublicMetrics() {
			return new CachePublicMetrics();
		}

	}

}
