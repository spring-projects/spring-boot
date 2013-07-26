/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.ops.autoconfigure;

import org.springframework.boot.config.EnableAutoConfiguration;
import org.springframework.boot.ops.metrics.CounterService;
import org.springframework.boot.ops.metrics.DefaultCounterService;
import org.springframework.boot.ops.metrics.DefaultGaugeService;
import org.springframework.boot.ops.metrics.GaugeService;
import org.springframework.boot.ops.metrics.InMemoryMetricRepository;
import org.springframework.boot.ops.metrics.MetricRepository;
import org.springframework.boot.strap.context.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics services.
 * 
 * @author Dave Syer
 */
@Configuration
public class MetricRepositoryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public CounterService counterService() {
		return new DefaultCounterService(metricRepository());
	}

	@Bean
	@ConditionalOnMissingBean
	public GaugeService gaugeService() {
		return new DefaultGaugeService(metricRepository());
	}

	@Bean
	@ConditionalOnMissingBean
	protected MetricRepository metricRepository() {
		return new InMemoryMetricRepository();
	}

}
