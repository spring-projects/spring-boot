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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.jersey;

import java.util.HashSet;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.DefaultEndpointPathProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.EndpointPathProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointOperation;
import org.springframework.boot.actuate.endpoint.web.jersey.JerseyEndpointResourceFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link ManagementContextConfiguration} for Jersey {@link Endpoint} concerns.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(ResourceConfig.class)
@ConditionalOnBean(ResourceConfig.class)
@ConditionalOnMissingBean(type = "org.springframework.web.servlet.DispatcherServlet")
class JerseyWebEndpointManagementContextConfiguration {

	@Bean
	public ResourceConfigCustomizer webEndpointRegistrar(
			EndpointProvider<WebEndpointOperation> provider,
			ManagementServerProperties managementServerProperties) {
		return (resourceConfig) -> resourceConfig.registerResources(
				new HashSet<>(new JerseyEndpointResourceFactory().createEndpointResources(
						new EndpointMapping(managementServerProperties.getContextPath()),
						provider.getEndpoints())));
	}

	@Bean
	@ConditionalOnMissingBean
	public EndpointPathProvider endpointPathProvider(
			EndpointProvider<WebEndpointOperation> provider,
			ManagementServerProperties managementServerProperties) {
		return new DefaultEndpointPathProvider(provider, managementServerProperties);
	}

}
