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

package org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.endpoint.web.WebEndpointOperation;
import org.springframework.boot.endpoint.web.jersey.JerseyEndpointResourceFactory;
import org.springframework.boot.endpoint.web.mvc.WebEndpointServletHandlerMapping;
import org.springframework.boot.endpoint.web.reactive.WebEndpointReactiveHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Management context configuration for the infrastructure for web endpoints.
 *
 * @author Andy Wilkinson
 */
@ConditionalOnWebApplication
@ManagementContextConfiguration
@EnableConfigurationProperties({ CorsEndpointProperties.class,
		ManagementServerProperties.class })
class WebEndpointInfrastructureManagementContextConfiguration {

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnClass(ResourceConfig.class)
	@ConditionalOnBean(ResourceConfig.class)
	@ConditionalOnMissingBean(type = "org.springframework.web.servlet.DispatcherServlet")
	static class JerseyWebEndpointConfiguration {

		@Bean
		public ResourceConfigCustomizer webEndpointRegistrar(
				EndpointProvider<WebEndpointOperation> provider,
				ManagementServerProperties managementServerProperties) {
			return (resourceConfig) -> resourceConfig.registerResources(new HashSet<>(
					new JerseyEndpointResourceFactory().createEndpointResources(
							managementServerProperties.getContextPath(),
							provider.getEndpoints())));
		}

	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnClass(DispatcherServlet.class)
	@ConditionalOnBean(DispatcherServlet.class)
	static class MvcWebEndpointConfiguration {

		private final List<WebEndpointHandlerMappingCustomizer> mappingCustomizers;

		MvcWebEndpointConfiguration(
				ObjectProvider<List<WebEndpointHandlerMappingCustomizer>> mappingCustomizers) {
			this.mappingCustomizers = mappingCustomizers
					.getIfUnique(Collections::emptyList);
		}

		@Bean
		@ConditionalOnMissingBean
		public WebEndpointServletHandlerMapping webEndpointServletHandlerMapping(
				EndpointProvider<WebEndpointOperation> provider,
				CorsEndpointProperties corsProperties,
				ManagementServerProperties managementServerProperties) {
			WebEndpointServletHandlerMapping handlerMapping = new WebEndpointServletHandlerMapping(
					managementServerProperties.getContextPath(), provider.getEndpoints(),
					getCorsConfiguration(corsProperties));
			for (WebEndpointHandlerMappingCustomizer customizer : this.mappingCustomizers) {
				customizer.customize(handlerMapping);
			}
			return handlerMapping;
		}

		private CorsConfiguration getCorsConfiguration(
				CorsEndpointProperties properties) {
			if (CollectionUtils.isEmpty(properties.getAllowedOrigins())) {
				return null;
			}
			CorsConfiguration configuration = new CorsConfiguration();
			configuration.setAllowedOrigins(properties.getAllowedOrigins());
			if (!CollectionUtils.isEmpty(properties.getAllowedHeaders())) {
				configuration.setAllowedHeaders(properties.getAllowedHeaders());
			}
			if (!CollectionUtils.isEmpty(properties.getAllowedMethods())) {
				configuration.setAllowedMethods(properties.getAllowedMethods());
			}
			if (!CollectionUtils.isEmpty(properties.getExposedHeaders())) {
				configuration.setExposedHeaders(properties.getExposedHeaders());
			}
			if (properties.getMaxAge() != null) {
				configuration.setMaxAge(properties.getMaxAge());
			}
			if (properties.getAllowCredentials() != null) {
				configuration.setAllowCredentials(properties.getAllowCredentials());
			}
			return configuration;
		}

	}

	@ConditionalOnWebApplication(type = Type.REACTIVE)
	static class ReactiveWebEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public WebEndpointReactiveHandlerMapping webEndpointReactiveHandlerMapping(
				EndpointProvider<WebEndpointOperation> provider,
				ManagementServerProperties managementServerProperties) {
			return new WebEndpointReactiveHandlerMapping(
					managementServerProperties.getContextPath(), provider.getEndpoints());
		}

	}

}
