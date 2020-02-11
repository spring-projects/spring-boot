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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.reactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.json.ActuatorJsonMapperProvider;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.reactive.ControllerEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.reactive.WebFluxEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Reactive
 * {@link Endpoint @Endpoint} concerns.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 2.0.0
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass({ DispatcherHandler.class, HttpHandler.class })
@ConditionalOnBean(WebEndpointsSupplier.class)
@EnableConfigurationProperties(CorsEndpointProperties.class)
public class WebFluxEndpointManagementContextConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WebFluxEndpointHandlerMapping webEndpointReactiveHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
			ControllerEndpointsSupplier controllerEndpointsSupplier, EndpointMediaTypes endpointMediaTypes,
			CorsEndpointProperties corsProperties, WebEndpointProperties webEndpointProperties,
			Environment environment) {
		String basePath = webEndpointProperties.getBasePath();
		EndpointMapping endpointMapping = new EndpointMapping(basePath);
		Collection<ExposableWebEndpoint> endpoints = webEndpointsSupplier.getEndpoints();
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		allEndpoints.addAll(endpoints);
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		return new WebFluxEndpointHandlerMapping(endpointMapping, endpoints, endpointMediaTypes,
				corsProperties.toCorsConfiguration(), new EndpointLinksResolver(allEndpoints, basePath),
				shouldRegisterLinksMapping(environment, basePath));
	}

	private boolean shouldRegisterLinksMapping(Environment environment, String basePath) {
		return StringUtils.hasText(basePath)
				|| ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT);
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

	@Bean
	@Order(-1)
	public CodecCustomizer actuatorJsonCodec(ActuatorJsonMapperProvider actuatorJsonMapperProvider) {
		return (configurer) -> {
			MediaType v3MediaType = MediaType.parseMediaType(ActuatorMediaType.V3_JSON);
			MediaType v2MediaType = MediaType.parseMediaType(ActuatorMediaType.V2_JSON);
			CodecConfigurer.CustomCodecs customCodecs = configurer.customCodecs();
			customCodecs.register(
					new Jackson2JsonEncoder(actuatorJsonMapperProvider.getInstance(), v3MediaType, v2MediaType));
		};
	}

}
