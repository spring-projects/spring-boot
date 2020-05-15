/*
 * Copyright 2012-2020 the original author or authors.
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

import com.datastax.oss.driver.api.core.CqlSession;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.cassandra.CassandraMetricsBinder;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link CqlSession Cassandra sessions}.
 *
 * @author Erik Merkle
 * @since 2.4.0
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
		CassandraAutoConfiguration.class })
@ConditionalOnClass({ MeterRegistry.class, CqlSession.class })
@ConditionalOnBean({ MeterRegistry.class, CqlSession.class })
public class CassandraMetricsAutoConfiguration {

	@Autowired
	public void bindCqlSessionMetricsToRegistry(CqlSession cqlSession, MeterRegistry registry) {

		// Don't bind to the MeterRegistry if the session has no metrics configured
		if (!cqlSession.getMetrics().isPresent()) {
			return;
		}
		CassandraMetricsBinder cassandraMetrics = new CassandraMetricsBinder(cqlSession);
		cassandraMetrics.bindTo(registry);
	}

}
