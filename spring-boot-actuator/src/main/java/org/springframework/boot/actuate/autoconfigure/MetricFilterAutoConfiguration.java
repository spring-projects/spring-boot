/*
 * Copyright 2012-2017 the original author or authors.
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

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} that records Servlet interactions
 * with a {@link CounterService} and {@link GaugeService}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Sebastian Kirsch
 */
@Configuration
@ConditionalOnBean({ CounterService.class, GaugeService.class })
@ConditionalOnClass({ Servlet.class, ServletRegistration.class,
		OncePerRequestFilter.class, HandlerMapping.class })
@AutoConfigureAfter(MetricRepositoryAutoConfiguration.class)
@ConditionalOnProperty(prefix = "endpoints.metrics.filter", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({ MetricFilterProperties.class })
public class MetricFilterAutoConfiguration {

	private final CounterService counterService;

	private final GaugeService gaugeService;

	private final MetricFilterProperties properties;

	public MetricFilterAutoConfiguration(CounterService counterService,
			GaugeService gaugeService, MetricFilterProperties properties) {
		this.counterService = counterService;
		this.gaugeService = gaugeService;
		this.properties = properties;
	}

	@Bean
	public MetricsFilter metricsFilter() {
		return new MetricsFilter(this.counterService, this.gaugeService, this.properties);
	}

}
