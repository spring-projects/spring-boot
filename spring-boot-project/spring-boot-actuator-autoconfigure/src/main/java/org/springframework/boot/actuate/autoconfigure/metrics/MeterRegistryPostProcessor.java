/*
 * Copyright 2012-2018 the original author or authors.
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor} that delegates to a lazily created
 * {@link MeterRegistryConfigurer} to post-process {@link MeterRegistry} beans.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class MeterRegistryPostProcessor implements BeanPostProcessor {

	private final ObjectProvider<MeterBinder> meterBinders;

	private final ObjectProvider<MeterFilter> meterFilters;

	private final ObjectProvider<MeterRegistryCustomizer<?>> meterRegistryCustomizers;

	private final ObjectProvider<MetricsProperties> metricsProperties;

	private volatile MeterRegistryConfigurer configurer;

	MeterRegistryPostProcessor(ObjectProvider<MeterBinder> meterBinders,
			ObjectProvider<MeterFilter> meterFilters,
			ObjectProvider<MeterRegistryCustomizer<?>> meterRegistryCustomizers,
			ObjectProvider<MetricsProperties> metricsProperties) {
		this.meterBinders = meterBinders;
		this.meterFilters = meterFilters;
		this.meterRegistryCustomizers = meterRegistryCustomizers;
		this.metricsProperties = metricsProperties;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof MeterRegistry) {
			getConfigurer().configure((MeterRegistry) bean);
		}
		return bean;
	}

	private MeterRegistryConfigurer getConfigurer() {
		if (this.configurer == null) {
			this.configurer = new MeterRegistryConfigurer(this.meterRegistryCustomizers,
					this.meterFilters, this.meterBinders,
					this.metricsProperties.getObject().isUseGlobalRegistry());
		}
		return this.configurer;
	}

}
