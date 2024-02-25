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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.servlet.ServletContainer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.jersey.JerseyHealthEndpointAdditionalPathResourceFactory;
import org.springframework.boot.actuate.endpoint.web.servlet.AdditionalHealthEndpointPathsWebMvcHandlerMapping;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jersey.JerseyProperties;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Configuration for {@link HealthEndpoint} web extensions.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnBean(HealthEndpoint.class)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class,
		exposure = { EndpointExposure.WEB, EndpointExposure.CLOUD_FOUNDRY })
class HealthEndpointWebExtensionConfiguration {

	/**
     * Creates a new instance of {@link HealthEndpointWebExtension} if no other bean of the same type is present.
     * 
     * @param healthContributorRegistry the {@link HealthContributorRegistry} used to retrieve health contributors
     * @param groups the {@link HealthEndpointGroups} used to group health contributors
     * @param properties the {@link HealthEndpointProperties} used to configure the health endpoint
     * @return a new instance of {@link HealthEndpointWebExtension}
     */
    @Bean
	@ConditionalOnMissingBean
	HealthEndpointWebExtension healthEndpointWebExtension(HealthContributorRegistry healthContributorRegistry,
			HealthEndpointGroups groups, HealthEndpointProperties properties) {
		return new HealthEndpointWebExtension(healthContributorRegistry, groups,
				properties.getLogging().getSlowIndicatorThreshold());
	}

	/**
     * Returns the health endpoint from the given WebEndpointsSupplier.
     *
     * @param webEndpointsSupplier the supplier of web endpoints
     * @return the health endpoint
     * @throws IllegalStateException if no endpoint with the specified id is found
     */
    private static ExposableWebEndpoint getHealthEndpoint(WebEndpointsSupplier webEndpointsSupplier) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		return webEndpoints.stream()
			.filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
			.findFirst()
			.orElseThrow(
					() -> new IllegalStateException("No endpoint with id '%s' found".formatted(HealthEndpoint.ID)));
	}

	/**
     * MvcAdditionalHealthEndpointPathsConfiguration class.
     */
    @ConditionalOnBean(DispatcherServlet.class)
	@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
	static class MvcAdditionalHealthEndpointPathsConfiguration {

		/**
         * Creates a new {@link AdditionalHealthEndpointPathsWebMvcHandlerMapping} bean
         * for mapping additional health endpoint paths in a Spring MVC application.
         *
         * @param webEndpointsSupplier the supplier for web endpoints
         * @param groups the health endpoint groups
         * @return the {@link AdditionalHealthEndpointPathsWebMvcHandlerMapping} bean
         */
        @Bean
		AdditionalHealthEndpointPathsWebMvcHandlerMapping healthEndpointWebMvcHandlerMapping(
				WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups groups) {
			ExposableWebEndpoint health = getHealthEndpoint(webEndpointsSupplier);
			return new AdditionalHealthEndpointPathsWebMvcHandlerMapping(health,
					groups.getAllWithAdditionalPath(WebServerNamespace.SERVER));
		}

	}

	/**
     * JerseyAdditionalHealthEndpointPathsConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ResourceConfig.class)
	@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
	@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
	static class JerseyAdditionalHealthEndpointPathsConfiguration {

		/**
         * Creates and returns a JerseyAdditionalHealthEndpointPathsResourcesRegistrar bean.
         * 
         * @param webEndpointsSupplier The supplier for web endpoints.
         * @param healthEndpointGroups The health endpoint groups.
         * @return The JerseyAdditionalHealthEndpointPathsResourcesRegistrar bean.
         */
        @Bean
		JerseyAdditionalHealthEndpointPathsResourcesRegistrar jerseyAdditionalHealthEndpointPathsResourcesRegistrar(
				WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups healthEndpointGroups) {
			ExposableWebEndpoint health = getHealthEndpoint(webEndpointsSupplier);
			return new JerseyAdditionalHealthEndpointPathsResourcesRegistrar(health, healthEndpointGroups);
		}

		/**
         * JerseyInfrastructureConfiguration class.
         */
        @Configuration(proxyBeanMethods = false)
		@ConditionalOnMissingBean(ResourceConfig.class)
		@EnableConfigurationProperties(JerseyProperties.class)
		static class JerseyInfrastructureConfiguration {

			/**
             * Creates a new instance of {@link JerseyApplicationPath} if no bean of type {@link JerseyApplicationPath} is already present.
             * 
             * @param properties the {@link JerseyProperties} instance containing the application path
             * @param config the {@link ResourceConfig} instance containing the Jersey configuration
             * @return a new instance of {@link JerseyApplicationPath} with the specified application path and configuration
             */
            @Bean
			@ConditionalOnMissingBean(JerseyApplicationPath.class)
			JerseyApplicationPath jerseyApplicationPath(JerseyProperties properties, ResourceConfig config) {
				return new DefaultJerseyApplicationPath(properties.getApplicationPath(), config);
			}

			/**
             * Creates and configures a ResourceConfig object for the Jersey infrastructure.
             * 
             * @param resourceConfigCustomizers the object provider for ResourceConfigCustomizer instances
             * @return the configured ResourceConfig object
             */
            @Bean
			ResourceConfig resourceConfig(ObjectProvider<ResourceConfigCustomizer> resourceConfigCustomizers) {
				ResourceConfig resourceConfig = new ResourceConfig();
				resourceConfigCustomizers.orderedStream().forEach((customizer) -> customizer.customize(resourceConfig));
				return resourceConfig;
			}

			/**
             * Registers the Jersey servlet with the specified application path and resource configuration.
             * 
             * @param jerseyApplicationPath the application path for the Jersey servlet
             * @param resourceConfig the resource configuration for the Jersey servlet
             * @return the servlet registration bean for the Jersey servlet
             */
            @Bean
			ServletRegistrationBean<ServletContainer> jerseyServletRegistration(
					JerseyApplicationPath jerseyApplicationPath, ResourceConfig resourceConfig) {
				return new ServletRegistrationBean<>(new ServletContainer(resourceConfig),
						jerseyApplicationPath.getUrlMapping());
			}

		}

	}

	/**
     * JerseyAdditionalHealthEndpointPathsResourcesRegistrar class.
     */
    static class JerseyAdditionalHealthEndpointPathsResourcesRegistrar implements ResourceConfigCustomizer {

		private final ExposableWebEndpoint endpoint;

		private final HealthEndpointGroups groups;

		/**
         * Constructs a new JerseyAdditionalHealthEndpointPathsResourcesRegistrar with the specified parameters.
         *
         * @param endpoint the ExposableWebEndpoint to register
         * @param groups the HealthEndpointGroups to associate with the endpoint
         */
        JerseyAdditionalHealthEndpointPathsResourcesRegistrar(ExposableWebEndpoint endpoint,
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
         * Registers the additional health endpoint paths resources in the provided {@link ResourceConfig}.
         * 
         * @param config the {@link ResourceConfig} to register the resources in
         */
        private void register(ResourceConfig config) {
			EndpointMapping mapping = new EndpointMapping("");
			JerseyHealthEndpointAdditionalPathResourceFactory resourceFactory = new JerseyHealthEndpointAdditionalPathResourceFactory(
					WebServerNamespace.SERVER, this.groups);
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
         * @param resources the collection of resources to be registered
         * @param config the resource configuration to register the resources with
         */
        private void register(Collection<Resource> resources, ResourceConfig config) {
			config.registerResources(new HashSet<>(resources));
		}

	}

}
