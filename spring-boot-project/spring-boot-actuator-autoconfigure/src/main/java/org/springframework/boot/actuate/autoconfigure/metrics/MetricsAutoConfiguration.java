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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.List;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @since 2.0.0
 */
@AutoConfiguration(before = CompositeMeterRegistryAutoConfiguration.class)
@ConditionalOnClass(Timed.class)
@EnableConfigurationProperties(MetricsProperties.class)
public class MetricsAutoConfiguration {

	/**
	 * Returns a Clock bean for Micrometer metrics. If no Clock bean is already defined,
	 * it returns the system clock.
	 * @return the Clock bean for Micrometer metrics
	 */
	@Bean
	@ConditionalOnMissingBean
	public Clock micrometerClock() {
		return Clock.SYSTEM;
	}

	/**
	 * Creates a MeterRegistryPostProcessor bean.
	 *
	 * This method is responsible for creating a MeterRegistryPostProcessor bean, which is
	 * used to customize the MeterRegistry bean in the application context. It takes in
	 * various dependencies such as the ApplicationContext, MetricsProperties,
	 * MeterRegistryCustomizer, MeterFilter, and MeterBinder to configure the
	 * MeterRegistry bean.
	 * @param applicationContext The ApplicationContext object used to access the
	 * application context.
	 * @param metricsProperties The MetricsProperties object used to access the metrics
	 * properties.
	 * @param meterRegistryCustomizers The ObjectProvider of MeterRegistryCustomizer used
	 * to customize the MeterRegistry bean.
	 * @param meterFilters The ObjectProvider of MeterFilter used to filter the metrics.
	 * @param meterBinders The ObjectProvider of MeterBinder used to bind additional
	 * metrics to the MeterRegistry bean.
	 * @return The MeterRegistryPostProcessor bean.
	 */
	@Bean
	public static MeterRegistryPostProcessor meterRegistryPostProcessor(ApplicationContext applicationContext,
			ObjectProvider<MetricsProperties> metricsProperties,
			ObjectProvider<MeterRegistryCustomizer<?>> meterRegistryCustomizers,
			ObjectProvider<MeterFilter> meterFilters, ObjectProvider<MeterBinder> meterBinders) {
		return new MeterRegistryPostProcessor(applicationContext, metricsProperties, meterRegistryCustomizers,
				meterFilters, meterBinders);
	}

	/**
	 * Creates a {@link PropertiesMeterFilter} bean with the given
	 * {@link MetricsProperties}.
	 * @param properties the {@link MetricsProperties} to be used by the filter
	 * @return the created {@link PropertiesMeterFilter} bean
	 */
	@Bean
	@Order(0)
	public PropertiesMeterFilter propertiesMeterFilter(MetricsProperties properties) {
		return new PropertiesMeterFilter(properties);
	}

	/**
	 * Creates a MeterRegistryCloser bean that closes all MeterRegistry beans in the
	 * application context.
	 * @param meterRegistries the ObjectProvider of MeterRegistry beans
	 * @return the MeterRegistryCloser bean
	 */
	@Bean
	MeterRegistryCloser meterRegistryCloser(ObjectProvider<MeterRegistry> meterRegistries) {
		return new MeterRegistryCloser(meterRegistries.orderedStream().toList());
	}

	/**
	 * Ensures that {@link MeterRegistry meter registries} are closed early in the
	 * shutdown process.
	 */
	static class MeterRegistryCloser implements ApplicationListener<ContextClosedEvent> {

		private final List<MeterRegistry> meterRegistries;

		/**
		 * Closes the given list of MeterRegistries.
		 * @param meterRegistries the list of MeterRegistries to be closed
		 */
		MeterRegistryCloser(List<MeterRegistry> meterRegistries) {
			this.meterRegistries = meterRegistries;
		}

		/**
		 * This method is called when the application context is closed. It iterates
		 * through all the meter registries and closes them if they are not already
		 * closed.
		 * @param event The context closed event
		 */
		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			for (MeterRegistry meterRegistry : this.meterRegistries) {
				if (!meterRegistry.isClosed()) {
					meterRegistry.close();
				}
			}
		}

	}

}
