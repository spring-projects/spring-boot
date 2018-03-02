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

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanPostProcessor;
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

	private static final Log logger = LogFactory
			.getLog(HikariDataSourceMetricsPostProcessor.class);

	private final ApplicationContext context;

	private volatile MeterRegistry meterRegistry;

	HikariDataSourceMetricsPostProcessor(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof HikariDataSource) {
			bindMetricsRegistryToHikariDataSource(getMeterRegistry(),
					(HikariDataSource) bean);
		}
		return bean;
	}

	private void bindMetricsRegistryToHikariDataSource(MeterRegistry registry,
			HikariDataSource dataSource) {
		if (dataSource.getMetricRegistry() == null
				&& dataSource.getMetricsTrackerFactory() == null) {
			try {
				dataSource.setMetricsTrackerFactory(
						new MicrometerMetricsTrackerFactory(registry));
			}
			catch (Exception ex) {
				logger.warn("Failed to bind Hikari metrics: " + ex.getMessage());
			}
		}
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
