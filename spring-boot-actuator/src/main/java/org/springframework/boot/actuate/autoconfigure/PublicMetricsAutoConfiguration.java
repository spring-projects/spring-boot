/*
 * Copyright 2012-2014 the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.DataSourcePublicMetrics;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RichGaugeReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.SystemPublicMetrics;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGaugeReader;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link PublicMetrics}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
@Configuration
@AutoConfigureAfter({ DataSourceAutoConfiguration.class,
		MetricRepositoryAutoConfiguration.class })
@AutoConfigureBefore(EndpointAutoConfiguration.class)
public class PublicMetricsAutoConfiguration {

	@Autowired(required = false)
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

}
