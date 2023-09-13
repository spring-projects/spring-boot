/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.jersey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ContextResolver;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.jersey.ManagementContextResourceConfigCustomizer;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jackson.EndpointObjectMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.jersey.JerseyEndpointResourceFactory;
import org.springframework.boot.actuate.endpoint.web.jersey.JerseyHealthEndpointAdditionalPathResourceFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Jersey
 * {@link Endpoint @Endpoint} concerns.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Michael Simons
 * @author Madhura Bhave
 * @author HaiTao Zhang
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(ResourceConfig.class)
@ConditionalOnBean(WebEndpointsSupplier.class)
@ConditionalOnMissingBean(type = "org.springframework.web.servlet.DispatcherServlet")
class JerseyWebEndpointManagementContextConfiguration {

	private static final EndpointId HEALTH_ENDPOINT_ID = EndpointId.of("health");

	@Bean
	JerseyWebEndpointsResourcesRegistrar jerseyWebEndpointsResourcesRegistrar(Environment environment,
			WebEndpointsSupplier webEndpointsSupplier, ServletEndpointsSupplier servletEndpointsSupplier,
			EndpointMediaTypes endpointMediaTypes, WebEndpointProperties webEndpointProperties) {
		String basePath = webEndpointProperties.getBasePath();
		boolean shouldRegisterLinks = shouldRegisterLinksMapping(webEndpointProperties, environment, basePath);
		return new JerseyWebEndpointsResourcesRegistrar(webEndpointsSupplier, servletEndpointsSupplier,
				endpointMediaTypes, basePath, shouldRegisterLinks);
	}

	@Bean
	@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
	@ConditionalOnBean(HealthEndpoint.class)
	@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
	JerseyAdditionalHealthEndpointPathsManagementResourcesRegistrar jerseyDifferentPortAdditionalHealthEndpointPathsResourcesRegistrar(
			WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups healthEndpointGroups) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		ExposableWebEndpoint health = webEndpoints.stream()
			.filter((endpoint) -> endpoint.getEndpointId().equals(HEALTH_ENDPOINT_ID))
			.findFirst()
			.orElseThrow(
					() -> new IllegalStateException("No endpoint with id '%s' found".formatted(HEALTH_ENDPOINT_ID)));
		return new JerseyAdditionalHealthEndpointPathsManagementResourcesRegistrar(health, healthEndpointGroups);
	}

	@Bean
	@ConditionalOnBean(EndpointObjectMapper.class)
	ResourceConfigCustomizer endpointObjectMapperResourceConfigCustomizer(EndpointObjectMapper endpointObjectMapper) {
		return (config) -> config.register(new EndpointObjectMapperContextResolver(endpointObjectMapper),
				ContextResolver.class);
	}

	private boolean shouldRegisterLinksMapping(WebEndpointProperties properties, Environment environment,
			String basePath) {
		return properties.getDiscovery().isEnabled() && (StringUtils.hasText(basePath)
				|| ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
	}

	/**
	 * Register endpoints with the {@link ResourceConfig} for the management context.
	 */
	static class JerseyWebEndpointsResourcesRegistrar implements ManagementContextResourceConfigCustomizer {

		private final WebEndpointsSupplier webEndpointsSupplier;

		private final ServletEndpointsSupplier servletEndpointsSupplier;

		private final EndpointMediaTypes mediaTypes;

		private final String basePath;

		private final boolean shouldRegisterLinks;

		JerseyWebEndpointsResourcesRegistrar(WebEndpointsSupplier webEndpointsSupplier,
				ServletEndpointsSupplier servletEndpointsSupplier, EndpointMediaTypes endpointMediaTypes,
				String basePath, boolean shouldRegisterLinks) {
			this.webEndpointsSupplier = webEndpointsSupplier;
			this.servletEndpointsSupplier = servletEndpointsSupplier;
			this.mediaTypes = endpointMediaTypes;
			this.basePath = basePath;
			this.shouldRegisterLinks = shouldRegisterLinks;
		}

		@Override
		public void customize(ResourceConfig config) {
			register(config);
		}

		private void register(ResourceConfig config) {
			Collection<ExposableWebEndpoint> webEndpoints = this.webEndpointsSupplier.getEndpoints();
			Collection<ExposableServletEndpoint> servletEndpoints = this.servletEndpointsSupplier.getEndpoints();
			EndpointLinksResolver linksResolver = getLinksResolver(webEndpoints, servletEndpoints);
			EndpointMapping mapping = new EndpointMapping(this.basePath);
			Collection<Resource> endpointResources = new JerseyEndpointResourceFactory().createEndpointResources(
					mapping, webEndpoints, this.mediaTypes, linksResolver, this.shouldRegisterLinks);
			register(endpointResources, config);
		}

		private EndpointLinksResolver getLinksResolver(Collection<ExposableWebEndpoint> webEndpoints,
				Collection<ExposableServletEndpoint> servletEndpoints) {
			List<ExposableEndpoint<?>> endpoints = new ArrayList<>(webEndpoints.size() + servletEndpoints.size());
			endpoints.addAll(webEndpoints);
			endpoints.addAll(servletEndpoints);
			return new EndpointLinksResolver(endpoints, this.basePath);
		}

		private void register(Collection<Resource> resources, ResourceConfig config) {
			config.registerResources(new HashSet<>(resources));
		}

	}

	class JerseyAdditionalHealthEndpointPathsManagementResourcesRegistrar
			implements ManagementContextResourceConfigCustomizer {

		private final ExposableWebEndpoint endpoint;

		private final HealthEndpointGroups groups;

		JerseyAdditionalHealthEndpointPathsManagementResourcesRegistrar(ExposableWebEndpoint endpoint,
				HealthEndpointGroups groups) {
			this.endpoint = endpoint;
			this.groups = groups;
		}

		@Override
		public void customize(ResourceConfig config) {
			register(config);
		}

		private void register(ResourceConfig config) {
			EndpointMapping mapping = new EndpointMapping("");
			JerseyHealthEndpointAdditionalPathResourceFactory resourceFactory = new JerseyHealthEndpointAdditionalPathResourceFactory(
					WebServerNamespace.MANAGEMENT, this.groups);
			Collection<Resource> endpointResources = resourceFactory
				.createEndpointResources(mapping, Collections.singletonList(this.endpoint))
				.stream()
				.filter(Objects::nonNull)
				.toList();
			register(endpointResources, config);
		}

		private void register(Collection<Resource> resources, ResourceConfig config) {
			config.registerResources(new HashSet<>(resources));
		}

	}

	/**
	 * {@link ContextResolver} used to obtain the {@link ObjectMapper} that should be used
	 * for {@link OperationResponseBody} instances.
	 */
	@Priority(Priorities.USER - 100)
	private static final class EndpointObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

		private final EndpointObjectMapper endpointObjectMapper;

		private EndpointObjectMapperContextResolver(EndpointObjectMapper endpointObjectMapper) {
			this.endpointObjectMapper = endpointObjectMapper;
		}

		@Override
		public ObjectMapper getContext(Class<?> type) {
			return OperationResponseBody.class.isAssignableFrom(type) ? this.endpointObjectMapper.get() : null;
		}

	}

}
