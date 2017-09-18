/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web.client;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for {@link RestTemplate}-related metrics.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
public class RestTemplateMetricsConfiguration {

	@Bean
	@ConditionalOnMissingBean(RestTemplateExchangeTagsProvider.class)
	public DefaultRestTemplateExchangeTagsProvider restTemplateTagConfigurer() {
		return new DefaultRestTemplateExchangeTagsProvider();
	}

	@Bean
	public MetricsRestTemplateCustomizer metricsRestTemplateCustomizer(
			MeterRegistry meterRegistry,
			RestTemplateExchangeTagsProvider restTemplateTagConfigurer,
			MetricsProperties properties) {
		return new MetricsRestTemplateCustomizer(meterRegistry, restTemplateTagConfigurer,
				properties.getWeb().getClient().getRequestsMetricName(),
				properties.getWeb().getClient().isRecordRequestPercentiles());
	}

	@Bean
	public static BeanPostProcessor restTemplateInterceptorPostProcessor(
			ApplicationContext applicationContext) {
		return new MetricsInterceptorPostProcessor(applicationContext);
	}

	/**
	 * {@link BeanPostProcessor} to apply {@link MetricsRestTemplateCustomizer} to any
	 * directly registered {@link RestTemplate} beans.
	 */
	private static class MetricsInterceptorPostProcessor implements BeanPostProcessor {

		private final ApplicationContext applicationContext;

		private MetricsRestTemplateCustomizer customizer;

		MetricsInterceptorPostProcessor(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof RestTemplate) {
				getCustomizer().customize((RestTemplate) bean);
			}
			return bean;
		}

		private MetricsRestTemplateCustomizer getCustomizer() {
			if (this.customizer == null) {
				this.customizer = this.applicationContext
						.getBean(MetricsRestTemplateCustomizer.class);
			}
			return this.customizer;
		}

	}

}
