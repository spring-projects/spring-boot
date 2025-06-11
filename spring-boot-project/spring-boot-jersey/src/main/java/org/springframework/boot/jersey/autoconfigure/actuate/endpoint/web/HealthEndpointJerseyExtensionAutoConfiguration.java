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

package org.springframework.boot.jersey.autoconfigure.actuate.endpoint.web;

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
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jersey.actuate.endpoint.web.JerseyHealthEndpointAdditionalPathResourceFactory;
import org.springframework.boot.jersey.autoconfigure.DefaultJerseyApplicationPath;
import org.springframework.boot.jersey.autoconfigure.JerseyApplicationPath;
import org.springframework.boot.jersey.autoconfigure.JerseyProperties;
import org.springframework.boot.jersey.autoconfigure.ResourceConfigCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-Configuration for {@link HealthEndpoint} Jersey extension.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0.0
 * @see HealthEndpointAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnBean(HealthEndpoint.class)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
public class HealthEndpointJerseyExtensionAutoConfiguration {

	@Bean
	JerseyAdditionalHealthEndpointPathsResourcesRegistrar jerseyAdditionalHealthEndpointPathsResourcesRegistrar(
			WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups healthEndpointGroups) {
		ExposableWebEndpoint health = getHealthEndpoint(webEndpointsSupplier);
		return new JerseyAdditionalHealthEndpointPathsResourcesRegistrar(health, healthEndpointGroups);
	}

	private static ExposableWebEndpoint getHealthEndpoint(WebEndpointsSupplier webEndpointsSupplier) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		return webEndpoints.stream()
			.filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
			.findFirst()
			.orElse(null);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ResourceConfig.class)
	@EnableConfigurationProperties(JerseyProperties.class)
	static class JerseyInfrastructureConfiguration {

		@Bean
		@ConditionalOnMissingBean
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
		ServletRegistrationBean<ServletContainer> jerseyServletRegistration(JerseyApplicationPath jerseyApplicationPath,
				ResourceConfig resourceConfig) {
			return new ServletRegistrationBean<>(new ServletContainer(resourceConfig),
					jerseyApplicationPath.getUrlMapping());
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
				.createEndpointResources(mapping,
						(this.endpoint != null) ? Collections.singletonList(this.endpoint) : Collections.emptyList())
				.stream()
				.filter(Objects::nonNull)
				.toList();
			register(endpointResources, config);
		}

		private void register(Collection<Resource> resources, ResourceConfig config) {
			config.registerResources(new HashSet<>(resources));
		}

	}

}
