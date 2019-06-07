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

package org.springframework.boot.actuate.autoconfigure.metrics.jdbc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.jdbc.DataSourceUnwrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

/**
 * {@link BeanPostProcessor} that configures Hikari metrics. Such arrangement is necessary
 * because a {@link HikariDataSource} instance cannot be modified once its configuration
 * has completed.
 *
 * @author Stephane Nicoll
 */
class HikariDataSourceMetricsPostProcessor implements BeanPostProcessor, Ordered {

	private static final Log logger = LogFactory.getLog(HikariDataSourceMetricsPostProcessor.class);

	private final ApplicationContext context;

	private volatile MeterRegistry meterRegistry;

	HikariDataSourceMetricsPostProcessor(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		HikariDataSource hikariDataSource = determineHikariDataSource(bean);
		if (hikariDataSource != null) {
			bindMetricsRegistryToHikariDataSource(getMeterRegistry(), hikariDataSource);
		}
		return bean;
	}

	private HikariDataSource determineHikariDataSource(Object bean) {
		if (bean instanceof DataSource) {
			return DataSourceUnwrapper.unwrap((DataSource) bean, HikariDataSource.class);
		}
		return null;
	}

	private void bindMetricsRegistryToHikariDataSource(MeterRegistry registry, HikariDataSource dataSource) {
		if (!hasExisingMetrics(dataSource)) {
			try {
				dataSource.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));
			}
			catch (Exception ex) {
				logger.warn("Failed to bind Hikari metrics: " + ex.getMessage());
			}
		}
	}

	private boolean hasExisingMetrics(HikariDataSource dataSource) {
		return dataSource.getMetricRegistry() != null || dataSource.getMetricsTrackerFactory() != null;
	}

	private MeterRegistry getMeterRegistry() {
		if (this.meterRegistry == null) {
			this.meterRegistry = this.context.getBean(MeterRegistry.class);
		}
		return this.meterRegistry;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
