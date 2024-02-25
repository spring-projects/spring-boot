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

	/**
	 * Constructs a new instance of RepositoryMetricsAutoConfiguration with the specified
	 * MetricsProperties.
	 * @param properties the MetricsProperties to be used by the auto configuration
	 */
	public RepositoryMetricsAutoConfiguration(MetricsProperties properties) {
		this.properties = properties;
	}

	/**
	 * Creates a new instance of {@link DefaultRepositoryTagsProvider} if no other bean of
	 * type {@link RepositoryTagsProvider} is present.
	 * @return the {@link DefaultRepositoryTagsProvider} bean
	 */
	@Bean
	@ConditionalOnMissingBean(RepositoryTagsProvider.class)
	public DefaultRepositoryTagsProvider repositoryTagsProvider() {
		return new DefaultRepositoryTagsProvider();
	}

	/**
	 * Creates a MetricsRepositoryMethodInvocationListener bean if no other bean of the
	 * same type is present. This listener is responsible for tracking method invocations
	 * on repository interfaces and reporting metrics.
	 * @param registry The provider for the MeterRegistry bean, used for reporting
	 * metrics.
	 * @param tagsProvider The provider for the RepositoryTagsProvider bean, used for
	 * providing tags for metrics.
	 * @return The MetricsRepositoryMethodInvocationListener bean.
	 */
	@Bean
	@ConditionalOnMissingBean
	public MetricsRepositoryMethodInvocationListener metricsRepositoryMethodInvocationListener(
			ObjectProvider<MeterRegistry> registry, RepositoryTagsProvider tagsProvider) {
		Repository properties = this.properties.getData().getRepository();
		return new MetricsRepositoryMethodInvocationListener(registry::getObject, tagsProvider,
				properties.getMetricName(), new PropertiesAutoTimer(properties.getAutotime()));
	}

	/**
	 * Creates a MetricsRepositoryMethodInvocationListenerBeanPostProcessor bean.
	 * @param metricsRepositoryMethodInvocationListener the
	 * MetricsRepositoryMethodInvocationListener object provider
	 * @return the MetricsRepositoryMethodInvocationListenerBeanPostProcessor bean
	 */
	@Bean
	public static MetricsRepositoryMethodInvocationListenerBeanPostProcessor metricsRepositoryMethodInvocationListenerBeanPostProcessor(
			ObjectProvider<MetricsRepositoryMethodInvocationListener> metricsRepositoryMethodInvocationListener) {
		return new MetricsRepositoryMethodInvocationListenerBeanPostProcessor(
				SingletonSupplier.of(metricsRepositoryMethodInvocationListener::getObject));
	}

}
