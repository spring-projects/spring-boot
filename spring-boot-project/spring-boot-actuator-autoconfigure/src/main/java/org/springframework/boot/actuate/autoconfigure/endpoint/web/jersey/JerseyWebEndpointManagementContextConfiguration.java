/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.HashSet;
import java.util.List;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.jersey.JerseyEndpointResourceFactory;
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

	@Bean
	JerseyWebEndpointsResourcesRegistrar jerseyWebEndpointsResourcesRegistrar(Environment environment,
			ObjectProvider<ResourceConfig> resourceConfig, WebEndpointsSupplier webEndpointsSupplier,
			ServletEndpointsSupplier servletEndpointsSupplier, EndpointMediaTypes endpointMediaTypes,
			WebEndpointProperties webEndpointProperties) {
		String basePath = webEndpointProperties.getBasePath();
		boolean shouldRegisterLinks = shouldRegisterLinksMapping(environment, basePath);
		shouldRegisterLinksMapping(environment, basePath);
		return new JerseyWebEndpointsResourcesRegistrar(resourceConfig.getIfAvailable(), webEndpointsSupplier,
				servletEndpointsSupplier, endpointMediaTypes, basePath, shouldRegisterLinks);
	}

	private boolean shouldRegisterLinksMapping(Environment environment, String basePath) {
		return StringUtils.hasText(basePath)
				|| ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT);
	}

	/**
	 * Register endpoints with the {@link ResourceConfig}. The
	 * {@link ResourceConfigCustomizer} cannot be used because we don't want to apply
	 */
	static class JerseyWebEndpointsResourcesRegistrar {

		private final ResourceConfig resourceConfig;

		private final WebEndpointsSupplier webEndpointsSupplier;

		private final ServletEndpointsSupplier servletEndpointsSupplier;

		private final EndpointMediaTypes mediaTypes;

		private final String basePath;

		private final boolean shouldRegisterLinks;

		JerseyWebEndpointsResourcesRegistrar(ResourceConfig resourceConfig, WebEndpointsSupplier webEndpointsSupplier,
				ServletEndpointsSupplier servletEndpointsSupplier, EndpointMediaTypes endpointMediaTypes,
				String basePath, boolean shouldRegisterLinks) {
			super();
			this.resourceConfig = resourceConfig;
			this.webEndpointsSupplier = webEndpointsSupplier;
			this.servletEndpointsSupplier = servletEndpointsSupplier;
			this.mediaTypes = endpointMediaTypes;
			this.basePath = basePath;
			this.shouldRegisterLinks = shouldRegisterLinks;
			register();
		}

		private void register() {
			// We can't easily use @ConditionalOnBean because @AutoConfigureBefore is
			// not an option for management contexts. Instead we manually check if
			// the resource config bean exists
			if (this.resourceConfig == null) {
				return;
			}
			Collection<ExposableWebEndpoint> webEndpoints = this.webEndpointsSupplier.getEndpoints();
			Collection<ExposableServletEndpoint> servletEndpoints = this.servletEndpointsSupplier.getEndpoints();
			EndpointLinksResolver linksResolver = getLinksResolver(webEndpoints, servletEndpoints);
			EndpointMapping mapping = new EndpointMapping(this.basePath);
			JerseyEndpointResourceFactory resourceFactory = new JerseyEndpointResourceFactory();
			register(resourceFactory.createEndpointResources(mapping, webEndpoints, this.mediaTypes, linksResolver,
					this.shouldRegisterLinks));
		}

		private EndpointLinksResolver getLinksResolver(Collection<ExposableWebEndpoint> webEndpoints,
				Collection<ExposableServletEndpoint> servletEndpoints) {
			List<ExposableEndpoint<?>> endpoints = new ArrayList<>(webEndpoints.size() + servletEndpoints.size());
			endpoints.addAll(webEndpoints);
			endpoints.addAll(servletEndpoints);
			return new EndpointLinksResolver(endpoints, this.basePath);
		}

		private void register(Collection<Resource> resources) {
			this.resourceConfig.registerResources(new HashSet<>(resources));
		}

	}

}
