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

package org.springframework.boot.actuate.autoconfigure.observation.web.servlet;

import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsContributor;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.ConditionalOnMissingFilterBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.observation.ServerRequestObservationConvention;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring Web
 * MVC servlet-based request mappings.
 *
 * @author Brian Clozel
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @since 3.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class, ObservationAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ DispatcherServlet.class, Observation.class })
@ConditionalOnBean(ObservationRegistry.class)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
@SuppressWarnings("removal")
public class WebMvcObservationAutoConfiguration {

	private final MetricsProperties metricsProperties;

	private final ObservationProperties observationProperties;

	public WebMvcObservationAutoConfiguration(ObservationProperties observationProperties,
			MetricsProperties metricsProperties) {
		this.observationProperties = observationProperties;
		this.metricsProperties = metricsProperties;
	}

	@Bean
	@ConditionalOnMissingFilterBean
	public FilterRegistrationBean<ServerHttpObservationFilter> webMvcObservationFilter(ObservationRegistry registry,
			ObjectProvider<WebMvcTagsProvider> customTagsProvider,
			ObjectProvider<WebMvcTagsContributor> contributorsProvider) {

		String observationName = this.observationProperties.getHttp().getServer().getRequests().getName();
		String metricName = this.metricsProperties.getWeb().getServer().getRequest().getMetricName();
		String name = (observationName != null) ? observationName : metricName;
		ServerRequestObservationConvention convention = new DefaultServerRequestObservationConvention(name);
		WebMvcTagsProvider tagsProvider = customTagsProvider.getIfAvailable();
		List<WebMvcTagsContributor> contributors = contributorsProvider.orderedStream().toList();
		if (tagsProvider != null || contributors.size() > 0) {
			convention = new ServerRequestObservationConventionAdapter(name, tagsProvider, contributors);
		}
		ServerHttpObservationFilter filter = new ServerHttpObservationFilter(registry, convention);
		FilterRegistrationBean<ServerHttpObservationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnBean(MeterRegistry.class)
	static class MeterFilterConfiguration {

		@Bean
		@Order(0)
		MeterFilter metricsHttpServerUriTagFilter(MetricsProperties properties) {
			String metricName = properties.getWeb().getServer().getRequest().getMetricName();
			MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
					() -> String.format("Reached the maximum number of URI tags for '%s'.", metricName));
			return MeterFilter.maximumAllowableTags(metricName, "uri", properties.getWeb().getServer().getMaxUriTags(),
					filter);
		}

	}

}
