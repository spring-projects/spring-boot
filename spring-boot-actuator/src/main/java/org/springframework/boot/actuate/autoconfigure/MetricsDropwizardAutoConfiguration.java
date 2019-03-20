/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import com.codahale.metrics.MetricRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.dropwizard.DropwizardMetricServices;
import org.springframework.boot.actuate.metrics.dropwizard.ReservoirFactory;
import org.springframework.boot.actuate.metrics.reader.MetricRegistryMetricReader;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Dropwizard-based metrics.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(MetricRegistry.class)
@AutoConfigureBefore(MetricRepositoryAutoConfiguration.class)
public class MetricsDropwizardAutoConfiguration {

	private final ReservoirFactory reservoirFactory;

	public MetricsDropwizardAutoConfiguration(
			ObjectProvider<ReservoirFactory> reservoirFactory) {
		this.reservoirFactory = reservoirFactory.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean
	public MetricRegistry metricRegistry() {
		return new MetricRegistry();
	}

	@Bean
	@ConditionalOnMissingBean({ DropwizardMetricServices.class, CounterService.class,
			GaugeService.class })
	public DropwizardMetricServices dropwizardMetricServices(
			MetricRegistry metricRegistry) {
		if (this.reservoirFactory == null) {
			return new DropwizardMetricServices(metricRegistry);
		}
		else {
			return new DropwizardMetricServices(metricRegistry, this.reservoirFactory);
		}
	}

	@Bean
	public MetricReaderPublicMetrics dropwizardPublicMetrics(
			MetricRegistry metricRegistry) {
		MetricRegistryMetricReader reader = new MetricRegistryMetricReader(
				metricRegistry);
		return new MetricReaderPublicMetrics(reader);
	}

}
