/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.r2dbc;

import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.r2dbc.ConnectionPoolMetrics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ConnectionFactory R2DBC connection factories}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 2.3.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
		R2dbcAutoConfiguration.class })
@ConditionalOnClass({ ConnectionPool.class, MeterRegistry.class })
@ConditionalOnBean({ ConnectionFactory.class, MeterRegistry.class })
public class ConnectionPoolMetricsAutoConfiguration {

	@Autowired
	public void bindConnectionPoolsToRegistry(Map<String, ConnectionFactory> connectionFactories,
			MeterRegistry registry) {
		connectionFactories.forEach((beanName, connectionFactory) -> {
			ConnectionPool pool = extractPool(connectionFactory);
			if (pool != null) {
				new ConnectionPoolMetrics(pool, beanName, Tags.empty()).bindTo(registry);
			}
		});
	}

	private ConnectionPool extractPool(Object candidate) {
		if (candidate instanceof ConnectionPool connectionPool) {
			return connectionPool;
		}
		if (candidate instanceof Wrapped) {
			return extractPool(((Wrapped<?>) candidate).unwrap());
		}
		return null;
	}

}
