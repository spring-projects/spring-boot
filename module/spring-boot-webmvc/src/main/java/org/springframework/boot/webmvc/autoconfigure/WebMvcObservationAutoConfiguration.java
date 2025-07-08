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

package org.springframework.boot.webmvc.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingFilterBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.metrics.autoconfigure.MetricsProperties;
import org.springframework.boot.observation.autoconfigure.ObservationProperties;
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
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = { "org.springframework.boot.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
				"org.springframework.boot.metrics.autoconfigure.MetricsAutoConfiguration",
				"org.springframework.boot.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration",
				"org.springframework.boot.observation.autoconfigure.ObservationAutoConfiguration" })
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ DispatcherServlet.class, Observation.class })
@ConditionalOnBean(ObservationRegistry.class)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public class WebMvcObservationAutoConfiguration {

	@Bean
	@ConditionalOnMissingFilterBean
	public FilterRegistrationBean<ServerHttpObservationFilter> webMvcObservationFilter(ObservationRegistry registry,
			ObjectProvider<ServerRequestObservationConvention> customConvention,
			ObservationProperties observationProperties) {
		String name = observationProperties.getHttp().getServer().getRequests().getName();
		ServerRequestObservationConvention convention = customConvention
			.getIfAvailable(() -> new DefaultServerRequestObservationConvention(name));
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
		MeterFilter metricsHttpServerUriTagFilter(ObservationProperties observationProperties,
				MetricsProperties metricsProperties) {
			String name = observationProperties.getHttp().getServer().getRequests().getName();
			MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
					() -> String.format("Reached the maximum number of URI tags for '%s'.", name));
			return MeterFilter.maximumAllowableTags(name, "uri", metricsProperties.getWeb().getServer().getMaxUriTags(),
					filter);
		}

	}

}
