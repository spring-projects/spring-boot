/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Set;
import java.util.stream.Collectors;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.boot.actuate.autoconfigure.endpoint.ExposeExcludePropertyEndpointFilter;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.ServletEndpointRegistrar;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPathProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link ManagementContextConfiguration} for servlet endpoints.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.0.0
 */
@ManagementContextConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class ServletEndpointManagementContextConfiguration {

	@Bean
	public ExposeExcludePropertyEndpointFilter<ExposableServletEndpoint> servletExposeExcludePropertyEndpointFilter(
			WebEndpointProperties properties) {
		WebEndpointProperties.Exposure exposure = properties.getExposure();
		return new ExposeExcludePropertyEndpointFilter<>(ExposableServletEndpoint.class,
				exposure.getInclude(), exposure.getExclude());
	}

	@Configuration
	@ConditionalOnClass(DispatcherServlet.class)
	public class WebMvcServletEndpointManagementContextConfiguration {

		private final ApplicationContext context;

		public WebMvcServletEndpointManagementContextConfiguration(
				ApplicationContext context) {
			this.context = context;
		}

		@Bean
		public ServletEndpointRegistrar servletEndpointRegistrar(
				WebEndpointProperties properties,
				ServletEndpointsSupplier servletEndpointsSupplier) {
			DispatcherServletPathProvider servletPathProvider = this.context
					.getBean(DispatcherServletPathProvider.class);
			Set<String> cleanedPaths = getServletPaths(properties, servletPathProvider);
			return new ServletEndpointRegistrar(cleanedPaths,
					servletEndpointsSupplier.getEndpoints());
		}

		private Set<String> getServletPaths(WebEndpointProperties properties,
				DispatcherServletPathProvider servletPathProvider) {
			return servletPathProvider.getServletPaths().stream()
					.map((p) -> cleanServletPath(p) + properties.getBasePath())
					.collect(Collectors.toSet());
		}

		private String cleanServletPath(String servletPath) {
			if (StringUtils.hasText(servletPath) && servletPath.endsWith("/")) {
				return servletPath.substring(0, servletPath.length() - 1);
			}
			return servletPath;
		}

	}

	@Configuration
	@ConditionalOnClass(ResourceConfig.class)
	@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
	public class JerseyServletEndpointManagementContextConfiguration {

		@Bean
		public ServletEndpointRegistrar servletEndpointRegistrar(
				WebEndpointProperties properties,
				ServletEndpointsSupplier servletEndpointsSupplier) {
			return new ServletEndpointRegistrar(properties.getBasePath(),
					servletEndpointsSupplier.getEndpoints());
		}

	}

}
