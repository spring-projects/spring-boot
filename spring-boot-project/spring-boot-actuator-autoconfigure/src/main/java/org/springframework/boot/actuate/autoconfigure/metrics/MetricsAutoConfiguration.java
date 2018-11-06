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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(Timed.class)
@EnableConfigurationProperties(MetricsProperties.class)
@AutoConfigureBefore(CompositeMeterRegistryAutoConfiguration.class)
public class MetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Clock micrometerClock() {
		return Clock.SYSTEM;
	}

	@Bean
	public static MeterRegistryPostProcessor meterRegistryPostProcessor(
			ObjectProvider<MeterBinder> meterBinders,
			ObjectProvider<MeterFilter> meterFilters,
			ObjectProvider<MeterRegistryCustomizer<?>> meterRegistryCustomizers,
			ObjectProvider<MetricsProperties> metricsProperties) {
		return new MeterRegistryPostProcessor(meterBinders, meterFilters,
				meterRegistryCustomizers, metricsProperties);
	}

	@Bean
	@Order(0)
	public PropertiesMeterFilter propertiesMeterFilter(MetricsProperties properties) {
		return new PropertiesMeterFilter(properties);
	}

}
