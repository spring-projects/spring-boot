/*
 * Copyright 2012-2025 the original author or authors.
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

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingFilterBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationConvention;
import org.springframework.web.filter.ServerHttpObservationFilter;

/**
 * Observation filter configurations imported by
 * {@link WebMvcObservationAutoConfiguration}.
 *
 * @author Brian Clozel
 */
abstract class ObservationFilterConfigurations {

	static <T extends ServerHttpObservationFilter> FilterRegistrationBean<T> filterRegistration(T filter) {
		FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Tracer.class)
	static class TracingHeaderObservation {

		@Bean
		@ConditionalOnBooleanProperty("management.observations.http.server.requests.write-trace-header")
		@ConditionalOnBean(Tracer.class)
		@ConditionalOnMissingFilterBean({ ServerHttpObservationFilter.class, TraceHeaderObservationFilter.class })
		FilterRegistrationBean<TraceHeaderObservationFilter> webMvcObservationFilter(ObservationRegistry registry,
				Tracer tracer, ObjectProvider<ServerRequestObservationConvention> customConvention,
				ObservationProperties observationProperties) {
			String name = observationProperties.getHttp().getServer().getRequests().getName();
			ServerRequestObservationConvention convention = customConvention
				.getIfAvailable(() -> new DefaultServerRequestObservationConvention(name));
			TraceHeaderObservationFilter filter = new TraceHeaderObservationFilter(tracer, registry, convention);
			return filterRegistration(filter);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DefaultObservation {

		@Bean
		@ConditionalOnMissingFilterBean({ ServerHttpObservationFilter.class, TraceHeaderObservationFilter.class })
		FilterRegistrationBean<ServerHttpObservationFilter> webMvcObservationFilter(ObservationRegistry registry,
				ObjectProvider<ServerRequestObservationConvention> customConvention,
				ObservationProperties observationProperties) {
			String name = observationProperties.getHttp().getServer().getRequests().getName();
			ServerRequestObservationConvention convention = customConvention
				.getIfAvailable(() -> new DefaultServerRequestObservationConvention(name));
			ServerHttpObservationFilter filter = new ServerHttpObservationFilter(registry, convention);
			return filterRegistration(filter);
		}

	}

}
