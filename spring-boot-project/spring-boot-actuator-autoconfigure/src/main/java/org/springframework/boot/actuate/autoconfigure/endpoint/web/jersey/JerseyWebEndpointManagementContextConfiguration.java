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

	/**
     * Creates and returns a JerseyWebEndpointsResourcesRegistrar bean.
     * 
     * @param environment The environment object.
     * @param webEndpointsSupplier The supplier for web endpoints.
     * @param servletEndpointsSupplier The supplier for servlet endpoints.
     * @param endpointMediaTypes The media types for the endpoints.
     * @param webEndpointProperties The properties for web endpoints.
     * @return The created JerseyWebEndpointsResourcesRegistrar bean.
     */
    @Bean
	JerseyWebEndpointsResourcesRegistrar jerseyWebEndpointsResourcesRegistrar(Environment environment,
			WebEndpointsSupplier webEndpointsSupplier, ServletEndpointsSupplier servletEndpointsSupplier,
			EndpointMediaTypes endpointMediaTypes, WebEndpointProperties webEndpointProperties) {
		String basePath = webEndpointProperties.getBasePath();
		boolean shouldRegisterLinks = shouldRegisterLinksMapping(webEndpointProperties, environment, basePath);
		return new JerseyWebEndpointsResourcesRegistrar(webEndpointsSupplier, servletEndpointsSupplier,
				endpointMediaTypes, basePath, shouldRegisterLinks);
	}

	/**
     * Registers additional management resources for the health endpoint on a different port using Jersey.
     * This method is conditionally executed based on the presence of a management port of type DIFFERENT,
     * the presence of a HealthEndpoint bean, and the availability of the HealthEndpoint as a web endpoint.
     * 
     * @param webEndpointsSupplier The supplier for web endpoints.
     * @param healthEndpointGroups The groups of health endpoints.
     * @return The registrar for additional health endpoint paths management resources.
     * @throws IllegalStateException if no endpoint with the specified id is found.
     */
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

	/**
     * Returns a ResourceConfigCustomizer that registers an EndpointObjectMapperContextResolver
     * with the provided EndpointObjectMapper as a ContextResolver.
     * 
     * @param endpointObjectMapper the EndpointObjectMapper to be used by the EndpointObjectMapperContextResolver
     * @return the ResourceConfigCustomizer that registers the EndpointObjectMapperContextResolver
     */
    @Bean
	@ConditionalOnBean(EndpointObjectMapper.class)
	ResourceConfigCustomizer endpointObjectMapperResourceConfigCustomizer(EndpointObjectMapper endpointObjectMapper) {
		return (config) -> config.register(new EndpointObjectMapperContextResolver(endpointObjectMapper),
				ContextResolver.class);
	}

	/**
     * Determines whether links mapping should be registered based on the provided properties, environment, and base path.
     * 
     * @param properties the web endpoint properties
     * @param environment the environment
     * @param basePath the base path
     * @return {@code true} if links mapping should be registered, {@code false} otherwise
     */
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

		/**
         * Constructs a new instance of JerseyWebEndpointsResourcesRegistrar with the specified parameters.
         *
         * @param webEndpointsSupplier the supplier for web endpoints
         * @param servletEndpointsSupplier the supplier for servlet endpoints
         * @param endpointMediaTypes the media types for the endpoints
         * @param basePath the base path for the endpoints
         * @param shouldRegisterLinks true if links should be registered, false otherwise
         */
        JerseyWebEndpointsResourcesRegistrar(WebEndpointsSupplier webEndpointsSupplier,
				ServletEndpointsSupplier servletEndpointsSupplier, EndpointMediaTypes endpointMediaTypes,
				String basePath, boolean shouldRegisterLinks) {
			this.webEndpointsSupplier = webEndpointsSupplier;
			this.servletEndpointsSupplier = servletEndpointsSupplier;
			this.mediaTypes = endpointMediaTypes;
			this.basePath = basePath;
			this.shouldRegisterLinks = shouldRegisterLinks;
		}

		/**
         * This method is used to customize the ResourceConfig object.
         * It registers the configuration with the provided ResourceConfig object.
         *
         * @param config the ResourceConfig object to be customized
         */
        @Override
		public void customize(ResourceConfig config) {
			register(config);
		}

		/**
         * Registers the Jersey endpoint resources with the given configuration.
         * 
         * @param config the resource configuration to register the endpoint resources with
         */
        private void register(ResourceConfig config) {
			Collection<ExposableWebEndpoint> webEndpoints = this.webEndpointsSupplier.getEndpoints();
			Collection<ExposableServletEndpoint> servletEndpoints = this.servletEndpointsSupplier.getEndpoints();
			EndpointLinksResolver linksResolver = getLinksResolver(webEndpoints, servletEndpoints);
			EndpointMapping mapping = new EndpointMapping(this.basePath);
			Collection<Resource> endpointResources = new JerseyEndpointResourceFactory().createEndpointResources(
					mapping, webEndpoints, this.mediaTypes, linksResolver, this.shouldRegisterLinks);
			register(endpointResources, config);
		}

		/**
         * Returns an instance of EndpointLinksResolver that resolves links for the given web endpoints and servlet endpoints.
         * 
         * @param webEndpoints the collection of web endpoints
         * @param servletEndpoints the collection of servlet endpoints
         * @return an instance of EndpointLinksResolver
         */
        private EndpointLinksResolver getLinksResolver(Collection<ExposableWebEndpoint> webEndpoints,
				Collection<ExposableServletEndpoint> servletEndpoints) {
			List<ExposableEndpoint<?>> endpoints = new ArrayList<>(webEndpoints.size() + servletEndpoints.size());
			endpoints.addAll(webEndpoints);
			endpoints.addAll(servletEndpoints);
			return new EndpointLinksResolver(endpoints, this.basePath);
		}

		/**
         * Registers a collection of resources with the given configuration.
         * 
         * @param resources the collection of resources to be registered
         * @param config the resource configuration to be used for registration
         */
        private void register(Collection<Resource> resources, ResourceConfig config) {
			config.registerResources(new HashSet<>(resources));
		}

	}

	/**
     * JerseyAdditionalHealthEndpointPathsManagementResourcesRegistrar class.
     */
    class JerseyAdditionalHealthEndpointPathsManagementResourcesRegistrar
			implements ManagementContextResourceConfigCustomizer {

		private final ExposableWebEndpoint endpoint;

		private final HealthEndpointGroups groups;

		/**
         * Constructs a new JerseyAdditionalHealthEndpointPathsManagementResourcesRegistrar with the specified
         * ExposableWebEndpoint and HealthEndpointGroups.
         *
         * @param endpoint the ExposableWebEndpoint to be used
         * @param groups the HealthEndpointGroups to be used
         */
        JerseyAdditionalHealthEndpointPathsManagementResourcesRegistrar(ExposableWebEndpoint endpoint,
				HealthEndpointGroups groups) {
			this.endpoint = endpoint;
			this.groups = groups;
		}

		/**
         * Customizes the given ResourceConfig object.
         * 
         * @param config the ResourceConfig object to be customized
         */
        @Override
		public void customize(ResourceConfig config) {
			register(config);
		}

		/**
         * Registers the additional health endpoint paths management resources.
         * 
         * @param config the resource configuration
         */
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

		/**
         * Registers a collection of resources with the given configuration.
         *
         * @param resources the collection of resources to register
         * @param config the resource configuration to use for registration
         */
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

		/**
         * Constructs a new EndpointObjectMapperContextResolver with the specified EndpointObjectMapper.
         * 
         * @param endpointObjectMapper the EndpointObjectMapper to be used by this context resolver
         */
        private EndpointObjectMapperContextResolver(EndpointObjectMapper endpointObjectMapper) {
			this.endpointObjectMapper = endpointObjectMapper;
		}

		/**
         * Returns the ObjectMapper instance based on the given class type.
         * 
         * @param type the class type for which the ObjectMapper instance is requested
         * @return the ObjectMapper instance if the given class type is assignable from OperationResponseBody class, otherwise null
         */
        @Override
		public ObjectMapper getContext(Class<?> type) {
			return OperationResponseBody.class.isAssignableFrom(type) ? this.endpointObjectMapper.get() : null;
		}

	}

}
