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

package org.springframework.boot.actuate.autoconfigure.metrics.data;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Data.Repository;
import org.springframework.boot.actuate.autoconfigure.metrics.PropertiesAutoTimer;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.data.DefaultRepositoryTagsProvider;
import org.springframework.boot.actuate.metrics.data.MetricsRepositoryMethodInvocationListener;
import org.springframework.boot.actuate.metrics.data.RepositoryTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Repository metrics.
 *
 * @author Phillip Webb
 * @since 2.5.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnClass(org.springframework.data.repository.Repository.class)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties(MetricsProperties.class)
public class RepositoryMetricsAutoConfiguration {

	private final MetricsProperties properties;

	public RepositoryMetricsAutoConfiguration(MetricsProperties properties) {
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
		Repository properties = this.properties.getData().getRepository();
		return new MetricsRepositoryMethodInvocationListener(registry::getObject, tagsProvider,
				properties.getMetricName(), new PropertiesAutoTimer(properties.getAutotime()));
	}

	@Bean
	static MetricsRepositoryMethodInvocationListenerBeanPostProcessor metricsRepositoryMethodInvocationListenerBeanPostProcessor(
			ObjectProvider<MetricsRepositoryMethodInvocationListener> metricsRepositoryMethodInvocationListener) {
		return new MetricsRepositoryMethodInvocationListenerBeanPostProcessor(
				SingletonSupplier.of(metricsRepositoryMethodInvocationListener::getObject));
	}

}
