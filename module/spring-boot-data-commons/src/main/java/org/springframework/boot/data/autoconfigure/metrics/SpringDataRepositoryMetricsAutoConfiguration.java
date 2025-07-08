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

package org.springframework.boot.data.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.autoconfigure.metrics.DataMetricsProperties.Repository;
import org.springframework.boot.data.metrics.DefaultRepositoryTagsProvider;
import org.springframework.boot.data.metrics.MetricsRepositoryMethodInvocationListener;
import org.springframework.boot.data.metrics.RepositoryTagsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Repository metrics.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = { "org.springframework.boot.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
				"org.springframework.boot.metrics.autoconfigure.MetricsAutoConfiguration",
				"org.springframework.boot.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration" })
@ConditionalOnClass(org.springframework.data.repository.Repository.class)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties(DataMetricsProperties.class)
public class SpringDataRepositoryMetricsAutoConfiguration {

	private final DataMetricsProperties properties;

	public SpringDataRepositoryMetricsAutoConfiguration(DataMetricsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(RepositoryTagsProvider.class)
	public DefaultRepositoryTagsProvider repositoryTagsProvider() {
		return new DefaultRepositoryTagsProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public MetricsRepositoryMethodInvocationListener metricsRepositoryMethodInvocationListener(
			ObjectProvider<MeterRegistry> registry, RepositoryTagsProvider tagsProvider) {
		Repository properties = this.properties.getRepository();
		return new MetricsRepositoryMethodInvocationListener(registry::getObject, tagsProvider,
				properties.getMetricName(), new PropertiesAutoTimer(properties.getAutotime()));
	}

	@Bean
	public static MetricsRepositoryMethodInvocationListenerBeanPostProcessor metricsRepositoryMethodInvocationListenerBeanPostProcessor(
			ObjectProvider<MetricsRepositoryMethodInvocationListener> metricsRepositoryMethodInvocationListener) {
		return new MetricsRepositoryMethodInvocationListenerBeanPostProcessor(
				SingletonSupplier.of(metricsRepositoryMethodInvocationListener::getObject));
	}

}
