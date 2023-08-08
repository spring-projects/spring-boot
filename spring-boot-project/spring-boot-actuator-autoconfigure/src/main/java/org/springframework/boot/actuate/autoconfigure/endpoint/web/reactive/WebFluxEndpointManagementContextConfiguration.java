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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jackson.EndpointObjectMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.reactive.AdditionalHealthEndpointPathsWebFluxHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.reactive.ControllerEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.reactive.WebFluxEndpointHandlerMapping;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.codec.Encoder;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Reactive
 * {@link Endpoint @Endpoint} concerns.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
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
				shouldRegisterLinksMapping(webEndpointProperties, environment, basePath));
	}

	private boolean shouldRegisterLinksMapping(WebEndpointProperties properties, Environment environment,
			String basePath) {
		return properties.getDiscovery().isEnabled() && (StringUtils.hasText(basePath)
				|| ManagementPortType.get(environment) == ManagementPortType.DIFFERENT);
	}

	@Bean
	@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
	@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
	@ConditionalOnBean(HealthEndpoint.class)
	public AdditionalHealthEndpointPathsWebFluxHandlerMapping managementHealthEndpointWebFluxHandlerMapping(
			WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups groups) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		ExposableWebEndpoint health = webEndpoints.stream()
			.filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
			.findFirst()
			.orElseThrow(
					() -> new IllegalStateException("No endpoint with id '%s' found".formatted(HealthEndpoint.ID)));
		return new AdditionalHealthEndpointPathsWebFluxHandlerMapping(new EndpointMapping(""), health,
				groups.getAllWithAdditionalPath(WebServerNamespace.MANAGEMENT));
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
	@ConditionalOnBean(EndpointObjectMapper.class)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static ServerCodecConfigurerEndpointObjectMapperBeanPostProcessor serverCodecConfigurerEndpointObjectMapperBeanPostProcessor(
			ObjectProvider<EndpointObjectMapper> endpointObjectMapper) {
		return new ServerCodecConfigurerEndpointObjectMapperBeanPostProcessor(
				SingletonSupplier.of(endpointObjectMapper::getObject));
	}

	/**
	 * {@link BeanPostProcessor} to apply {@link EndpointObjectMapper} for
	 * {@link OperationResponseBody} to {@link Jackson2JsonEncoder} instances.
	 */
	static class ServerCodecConfigurerEndpointObjectMapperBeanPostProcessor implements BeanPostProcessor {

		private static final List<MediaType> MEDIA_TYPES = Collections
			.unmodifiableList(Arrays.asList(MediaType.APPLICATION_JSON, new MediaType("application", "*+json")));

		private final Supplier<EndpointObjectMapper> endpointObjectMapper;

		ServerCodecConfigurerEndpointObjectMapperBeanPostProcessor(
				Supplier<EndpointObjectMapper> endpointObjectMapper) {
			this.endpointObjectMapper = endpointObjectMapper;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof ServerCodecConfigurer serverCodecConfigurer) {
				process(serverCodecConfigurer);
			}
			return bean;
		}

		private void process(ServerCodecConfigurer configurer) {
			for (HttpMessageWriter<?> writer : configurer.getWriters()) {
				if (writer instanceof EncoderHttpMessageWriter<?> encoderHttpMessageWriter) {
					process((encoderHttpMessageWriter).getEncoder());
				}
			}
		}

		private void process(Encoder<?> encoder) {
			if (encoder instanceof Jackson2JsonEncoder jackson2JsonEncoder) {
				jackson2JsonEncoder.registerObjectMappersForType(OperationResponseBody.class, (associations) -> {
					ObjectMapper objectMapper = this.endpointObjectMapper.get().get();
					MEDIA_TYPES.forEach((mimeType) -> associations.put(mimeType, objectMapper));
				});
			}
		}

	}

}
