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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.config.BeanDefinition;
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
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.AdditionalHealthEndpointPathsWebMvcHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.servlet.ControllerEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
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
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

	/**
     * Creates a {@link WebMvcEndpointHandlerMapping} bean for mapping web endpoints.
     * This bean is conditional on the absence of any existing bean of the same type.
     * 
     * @param webEndpointsSupplier the supplier for web endpoints
     * @param servletEndpointsSupplier the supplier for servlet endpoints
     * @param controllerEndpointsSupplier the supplier for controller endpoints
     * @param endpointMediaTypes the media types for the endpoints
     * @param corsProperties the CORS properties for the endpoints
     * @param webEndpointProperties the properties for web endpoints
     * @param environment the environment for the application
     * @return the created {@link WebMvcEndpointHandlerMapping} bean
     */
    @Bean
	@ConditionalOnMissingBean
	public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
			ServletEndpointsSupplier servletEndpointsSupplier, ControllerEndpointsSupplier controllerEndpointsSupplier,
			EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
			WebEndpointProperties webEndpointProperties, Environment environment) {
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		allEndpoints.addAll(webEndpoints);
		allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		String basePath = webEndpointProperties.getBasePath();
		EndpointMapping endpointMapping = new EndpointMapping(basePath);
		boolean shouldRegisterLinksMapping = shouldRegisterLinksMapping(webEndpointProperties, environment, basePath);
		return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints, endpointMediaTypes,
				corsProperties.toCorsConfiguration(), new EndpointLinksResolver(allEndpoints, basePath),
				shouldRegisterLinksMapping);
	}

	/**
     * Determines whether links mapping should be registered based on the provided parameters.
     * 
     * @param webEndpointProperties the properties for web endpoints
     * @param environment the environment
     * @param basePath the base path for the endpoints
     * @return true if links mapping should be registered, false otherwise
     */
    private boolean shouldRegisterLinksMapping(WebEndpointProperties webEndpointProperties, Environment environment,
			String basePath) {
		return webEndpointProperties.getDiscovery().isEnabled() && (StringUtils.hasText(basePath)
				|| ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
	}

	/**
     * Creates a {@link AdditionalHealthEndpointPathsWebMvcHandlerMapping} bean for mapping the management health endpoint
     * to additional paths in a web MVC environment.
     *
     * This bean is conditionally created based on the following conditions:
     * - The management port type is set to {@link ManagementPortType#DIFFERENT}.
     * - A bean of type {@link HealthEndpoint} is available in the application context.
     * - The {@link HealthEndpoint} is exposed as a web endpoint.
     *
     * The {@link AdditionalHealthEndpointPathsWebMvcHandlerMapping} bean is created by retrieving the collection of
     * {@link ExposableWebEndpoint} beans from the {@link WebEndpointsSupplier}. The health endpoint is then filtered
     * from the collection based on its endpoint ID. If the health endpoint is not found, an {@link IllegalStateException}
     * is thrown. Finally, the {@link AdditionalHealthEndpointPathsWebMvcHandlerMapping} bean is created using the
     * filtered health endpoint and the additional paths from the {@link HealthEndpointGroups} for the management
     * web server namespace.
     *
     * @param webEndpointsSupplier the supplier for the collection of {@link ExposableWebEndpoint} beans
     * @param groups the {@link HealthEndpointGroups} for the management web server namespace
     * @return the {@link AdditionalHealthEndpointPathsWebMvcHandlerMapping} bean for mapping the management health endpoint
     * to additional paths
     * @throws IllegalStateException if no health endpoint with the specified ID is found
     */
    @Bean
	@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
	@ConditionalOnBean(HealthEndpoint.class)
	@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
	public AdditionalHealthEndpointPathsWebMvcHandlerMapping managementHealthEndpointWebMvcHandlerMapping(
			WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups groups) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		ExposableWebEndpoint health = webEndpoints.stream()
			.filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
			.findFirst()
			.orElseThrow(
					() -> new IllegalStateException("No endpoint with id '%s' found".formatted(HealthEndpoint.ID)));
		return new AdditionalHealthEndpointPathsWebMvcHandlerMapping(health,
				groups.getAllWithAdditionalPath(WebServerNamespace.MANAGEMENT));
	}

	/**
     * Creates a {@link ControllerEndpointHandlerMapping} bean if there is no existing bean of the same type.
     * This bean is responsible for mapping controller endpoints to their respective handlers.
     * 
     * @param controllerEndpointsSupplier The supplier for obtaining the controller endpoints.
     * @param corsProperties The CORS properties for configuring CORS support.
     * @param webEndpointProperties The web endpoint properties for configuring the base path.
     * @return The created {@link ControllerEndpointHandlerMapping} bean.
     */
    @Bean
	@ConditionalOnMissingBean
	public ControllerEndpointHandlerMapping controllerEndpointHandlerMapping(
			ControllerEndpointsSupplier controllerEndpointsSupplier, CorsEndpointProperties corsProperties,
			WebEndpointProperties webEndpointProperties) {
		EndpointMapping endpointMapping = new EndpointMapping(webEndpointProperties.getBasePath());
		return new ControllerEndpointHandlerMapping(endpointMapping, controllerEndpointsSupplier.getEndpoints(),
				corsProperties.toCorsConfiguration());
	}

	/**
     * Creates a new instance of {@link EndpointObjectMapperWebMvcConfigurer} if an instance of {@link EndpointObjectMapper} is present in the application context.
     * This method is annotated with {@link ConditionalOnBean} to ensure that it is only executed if an instance of {@link EndpointObjectMapper} is available.
     * The created instance is then returned as a {@link Bean} and assigned the role of {@link BeanDefinition.ROLE_INFRASTRUCTURE}.
     *
     * @param endpointObjectMapper the {@link EndpointObjectMapper} instance to be used by the {@link EndpointObjectMapperWebMvcConfigurer}
     * @return a new instance of {@link EndpointObjectMapperWebMvcConfigurer} if an instance of {@link EndpointObjectMapper} is present, otherwise null
     */
    @Bean
	@ConditionalOnBean(EndpointObjectMapper.class)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static EndpointObjectMapperWebMvcConfigurer endpointObjectMapperWebMvcConfigurer(
			EndpointObjectMapper endpointObjectMapper) {
		return new EndpointObjectMapperWebMvcConfigurer(endpointObjectMapper);
	}

	/**
	 * {@link WebMvcConfigurer} to apply {@link EndpointObjectMapper} for
	 * {@link OperationResponseBody} to {@link MappingJackson2HttpMessageConverter}
	 * instances.
	 */
	static class EndpointObjectMapperWebMvcConfigurer implements WebMvcConfigurer {

		private static final List<MediaType> MEDIA_TYPES = Collections
			.unmodifiableList(Arrays.asList(MediaType.APPLICATION_JSON, new MediaType("application", "*+json")));

		private final EndpointObjectMapper endpointObjectMapper;

		/**
         * Constructs a new EndpointObjectMapperWebMvcConfigurer with the specified EndpointObjectMapper.
         * 
         * @param endpointObjectMapper the EndpointObjectMapper to be used by this EndpointObjectMapperWebMvcConfigurer
         */
        EndpointObjectMapperWebMvcConfigurer(EndpointObjectMapper endpointObjectMapper) {
			this.endpointObjectMapper = endpointObjectMapper;
		}

		/**
         * Configures the message converters for the EndpointObjectMapperWebMvcConfigurer class.
         * 
         * @param converters the list of HttpMessageConverters to be configured
         */
        @Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			for (HttpMessageConverter<?> converter : converters) {
				if (converter instanceof MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
					configure(mappingJackson2HttpMessageConverter);
				}
			}
		}

		/**
         * Configures the MappingJackson2HttpMessageConverter by registering object mappers for the specified type.
         * 
         * @param converter the MappingJackson2HttpMessageConverter to be configured
         */
        private void configure(MappingJackson2HttpMessageConverter converter) {
			converter.registerObjectMappersForType(OperationResponseBody.class, (associations) -> {
				ObjectMapper objectMapper = this.endpointObjectMapper.get();
				MEDIA_TYPES.forEach((mimeType) -> associations.put(mimeType, objectMapper));
			});
		}

	}

}
