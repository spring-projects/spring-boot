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
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationConvention;
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
			ObjectProvider<ServerRequestObservationConvention> customConvention,
			ObjectProvider<WebMvcTagsProvider> customTagsProvider,
			ObjectProvider<WebMvcTagsContributor> contributorsProvider) {
		String name = httpRequestsMetricName(this.observationProperties, this.metricsProperties);
		ServerRequestObservationConvention convention = createConvention(customConvention.getIfAvailable(), name,
				customTagsProvider.getIfAvailable(), contributorsProvider.orderedStream().toList());
		ServerHttpObservationFilter filter = new ServerHttpObservationFilter(registry, convention);
		FilterRegistrationBean<ServerHttpObservationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

	private static ServerRequestObservationConvention createConvention(
			ServerRequestObservationConvention customConvention, String name, WebMvcTagsProvider tagsProvider,
			List<WebMvcTagsContributor> contributors) {
		if (customConvention != null) {
			return customConvention;
		}
		else if (tagsProvider != null || contributors.size() > 0) {
			return new ServerRequestObservationConventionAdapter(name, tagsProvider, contributors);
		}
		else {
			return new DefaultServerRequestObservationConvention(name);
		}
	}

	private static String httpRequestsMetricName(ObservationProperties observationProperties,
			MetricsProperties metricsProperties) {
		String observationName = observationProperties.getHttp().getServer().getRequests().getName();
		return (observationName != null) ? observationName
				: metricsProperties.getWeb().getServer().getRequest().getMetricName();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnBean(MeterRegistry.class)
	static class MeterFilterConfiguration {

		@Bean
		@Order(0)
		MeterFilter metricsHttpServerUriTagFilter(MetricsProperties metricsProperties,
				ObservationProperties observationProperties) {
			String name = httpRequestsMetricName(observationProperties, metricsProperties);
			MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
					() -> String.format("Reached the maximum number of URI tags for '%s'.", name));
			return MeterFilter.maximumAllowableTags(name, "uri", metricsProperties.getWeb().getServer().getMaxUriTags(),
					filter);
		}

	}

}
