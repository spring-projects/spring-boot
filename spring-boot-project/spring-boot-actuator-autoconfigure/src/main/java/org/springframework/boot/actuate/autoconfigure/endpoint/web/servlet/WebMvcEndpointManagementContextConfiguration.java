/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.ControllerEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Spring MVC
 * {@link Endpoint @Endpoint} concerns.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean({ DispatcherServlet.class, WebEndpointsSupplier.class })
@EnableConfigurationProperties(CorsEndpointProperties.class)
public class WebMvcEndpointManagementContextConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
			ServletEndpointsSupplier servletEndpointsSupplier, ControllerEndpointsSupplier controllerEndpointsSupplier,
			EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
			WebEndpointProperties webEndpointProperties) {
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		allEndpoints.addAll(webEndpoints);
		allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		EndpointMapping endpointMapping = new EndpointMapping(webEndpointProperties.getBasePath());
		return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints, endpointMediaTypes,
				corsProperties.toCorsConfiguration(),
				new EndpointLinksResolver(allEndpoints, webEndpointProperties.getBasePath()));
	}

	@Bean
	@ConditionalOnMissingBean
	public ControllerEndpointHandlerMapping controllerEndpointHandlerMapping(
			ControllerEndpointsSupplier controllerEndpointsSupplier, CorsEndpointProperties corsProperties,
			WebEndpointProperties webEndpointProperties) {
		EndpointMapping endpointMapping = new EndpointMapping(webEndpointProperties.getBasePath());
		return new ControllerEndpointHandlerMapping(endpointMapping, controllerEndpointsSupplier.getEndpoints(),
				corsProperties.toCorsConfiguration());
	}

}
