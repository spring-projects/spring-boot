/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.stream.Collectors;

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
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
class HealthEndpointWebExtensionConfiguration {

	@Bean
	@ConditionalOnMissingBean
	HealthEndpointWebExtension healthEndpointWebExtension(HealthContributorRegistry healthContributorRegistry,
			HealthEndpointGroups groups) {
		return new HealthEndpointWebExtension(healthContributorRegistry, groups);
	}

	private static ExposableWebEndpoint getHealthEndpoint(WebEndpointsSupplier webEndpointsSupplier) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		return webEndpoints.stream().filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
				.findFirst().get();
	}

	@ConditionalOnBean(DispatcherServlet.class)
	static class MvcAdditionalHealthEndpointPathsConfiguration {

		@Bean
		AdditionalHealthEndpointPathsWebMvcHandlerMapping healthEndpointWebMvcHandlerMapping(
				WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups groups) {
			ExposableWebEndpoint health = getHealthEndpoint(webEndpointsSupplier);
			return new AdditionalHealthEndpointPathsWebMvcHandlerMapping(health,
					groups.getAllWithAdditionalPath(WebServerNamespace.SERVER));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ResourceConfig.class)
	@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
	static class JerseyAdditionalHealthEndpointPathsConfiguration {

		@Bean
		JerseyAdditionalHealthEndpointPathsResourcesRegistrar jerseyAdditionalHealthEndpointPathsResourcesRegistrar(
				WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups healthEndpointGroups) {
			ExposableWebEndpoint health = getHealthEndpoint(webEndpointsSupplier);
			return new JerseyAdditionalHealthEndpointPathsResourcesRegistrar(health, healthEndpointGroups);
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnMissingBean(ResourceConfig.class)
		@EnableConfigurationProperties(JerseyProperties.class)
		static class JerseyInfrastructureConfiguration {

			@Bean
			@ConditionalOnMissingBean(JerseyApplicationPath.class)
			JerseyApplicationPath jerseyApplicationPath(JerseyProperties properties, ResourceConfig config) {
				return new DefaultJerseyApplicationPath(properties.getApplicationPath(), config);
			}

			@Bean
			ResourceConfig resourceConfig(ObjectProvider<ResourceConfigCustomizer> resourceConfigCustomizers) {
				ResourceConfig resourceConfig = new ResourceConfig();
				resourceConfigCustomizers.orderedStream().forEach((customizer) -> customizer.customize(resourceConfig));
				return resourceConfig;
			}

			@Bean
			ServletRegistrationBean<ServletContainer> jerseyServletRegistration(
					JerseyApplicationPath jerseyApplicationPath, ResourceConfig resourceConfig) {
				return new ServletRegistrationBean<>(new ServletContainer(resourceConfig),
						jerseyApplicationPath.getUrlMapping());
			}

		}

	}

	static class JerseyAdditionalHealthEndpointPathsResourcesRegistrar implements ResourceConfigCustomizer {

		private final ExposableWebEndpoint endpoint;

		private final HealthEndpointGroups groups;

		JerseyAdditionalHealthEndpointPathsResourcesRegistrar(ExposableWebEndpoint endpoint,
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
					WebServerNamespace.SERVER, this.groups);
			Collection<Resource> endpointResources = resourceFactory
					.createEndpointResources(mapping, Collections.singletonList(this.endpoint), null, null, false)
					.stream().filter(Objects::nonNull).collect(Collectors.toList());
			register(endpointResources, config);
		}

		private void register(Collection<Resource> resources, ResourceConfig config) {
			config.registerResources(new HashSet<>(resources));
		}

	}

}
