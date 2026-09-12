/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.autoconfigure.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.PoolingHttpClientConnectionManagerMetricsBinder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.micrometer.metrics.MaximumAllowableTagsMeterFilter;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsProperties.Web.Client;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP client-related metrics.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Brian Clozel
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ ObservationProperties.class, MeterRegistry.class, MetricsProperties.class })
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public final class HttpClientMetricsAutoConfiguration {

	@Bean
	@Order(0)
	MaximumAllowableTagsMeterFilter metricsHttpClientUriTagFilter(ObservationProperties observationProperties,
			MetricsProperties metricsProperties) {
		Client clientProperties = metricsProperties.getWeb().getClient();
		String meterNamePrefix = observationProperties.getHttp().getClient().getRequests().getName();
		int maxUriTags = clientProperties.getMaxUriTags();
		return new MaximumAllowableTagsMeterFilter(meterNamePrefix, "uri", maxUriTags, "Are you using 'uriVariables'?");
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = { "org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager",
			"io.micrometer.core.instrument.binder.httpcomponents.hc5.PoolingHttpClientConnectionManagerMetricsBinder" })
	static HttpComponentsConnectionPoolMetricsBeanPostProcessor httpComponentsConnectionPoolMetricsBeanPostProcessor(
			ObjectProvider<MeterRegistry> meterRegistry) {
		return new HttpComponentsConnectionPoolMetricsBeanPostProcessor(meterRegistry);
	}

	/**
	 * {@link BeanPostProcessor} that customizes every
	 * {@link HttpComponentsClientHttpRequestFactoryBuilder} bean so that its
	 * {@link PoolingHttpClientConnectionManager} is bound to the application's
	 * {@link MeterRegistry}.
	 */
	@SuppressWarnings("deprecation")
	static final class HttpComponentsConnectionPoolMetricsBeanPostProcessor implements BeanPostProcessor, Ordered {

		private final Supplier<MeterRegistry> meterRegistry;

		private final AtomicInteger poolCounter = new AtomicInteger();

		HttpComponentsConnectionPoolMetricsBeanPostProcessor(ObjectProvider<MeterRegistry> meterRegistry) {
			this.meterRegistry = SingletonSupplier.of(meterRegistry::getObject);
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof HttpComponentsClientHttpRequestFactoryBuilder builder) {
				return builder.withConnectionManagerPostConfigurer(
						(connectionManager) -> bindToMeterRegistry(beanName, connectionManager));
			}
			return bean;
		}

		private void bindToMeterRegistry(String beanName, PoolingHttpClientConnectionManager connectionManager) {
			String poolName = beanName + ".pool-" + this.poolCounter.getAndIncrement();
			new PoolingHttpClientConnectionManagerMetricsBinder(connectionManager, poolName)
				.bindTo(this.meterRegistry.get());
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

	}

}
