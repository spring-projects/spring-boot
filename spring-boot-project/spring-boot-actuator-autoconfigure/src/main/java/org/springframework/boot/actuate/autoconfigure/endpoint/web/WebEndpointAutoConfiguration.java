/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for web {@link Endpoint @Endpoint}
 * support.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@AutoConfiguration(after = EndpointAutoConfiguration.class)
@ConditionalOnWebApplication
@EnableConfigurationProperties(WebEndpointProperties.class)
public class WebEndpointAutoConfiguration {

	private final ApplicationContext applicationContext;

	private final WebEndpointProperties properties;

	/**
	 * Constructs a new instance of the {@code WebEndpointAutoConfiguration} class.
	 * @param applicationContext the application context
	 * @param properties the web endpoint properties
	 */
	public WebEndpointAutoConfiguration(ApplicationContext applicationContext, WebEndpointProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	/**
	 * Returns a new instance of {@link PathMapper} configured with the provided path
	 * mapping.
	 * @return a new instance of {@link PathMapper}
	 */
	@Bean
	public PathMapper webEndpointPathMapper() {
		return new MappingWebEndpointPathMapper(this.properties.getPathMapping());
	}

	/**
	 * Returns the default EndpointMediaTypes if no bean of type EndpointMediaTypes is
	 * found.
	 * @return the default EndpointMediaTypes
	 */
	@Bean
	@ConditionalOnMissingBean
	public EndpointMediaTypes endpointMediaTypes() {
		return EndpointMediaTypes.DEFAULT;
	}

	/**
	 * Creates a {@link WebEndpointDiscoverer} bean if no other bean of type
	 * {@link WebEndpointsSupplier} is present. This bean is responsible for discovering
	 * and exposing web endpoints.
	 * @param parameterValueMapper the {@link ParameterValueMapper} used to map parameter
	 * values
	 * @param endpointMediaTypes the {@link EndpointMediaTypes} used to determine the
	 * media types supported by the endpoints
	 * @param endpointPathMappers the {@link PathMapper}s used to map endpoint paths
	 * @param invokerAdvisors the {@link OperationInvokerAdvisor}s used to advise the
	 * operation invokers
	 * @param filters the {@link EndpointFilter}s used to filter the discovered endpoints
	 * @return the {@link WebEndpointDiscoverer} bean
	 */
	@Bean
	@ConditionalOnMissingBean(WebEndpointsSupplier.class)
	public WebEndpointDiscoverer webEndpointDiscoverer(ParameterValueMapper parameterValueMapper,
			EndpointMediaTypes endpointMediaTypes, ObjectProvider<PathMapper> endpointPathMappers,
			ObjectProvider<OperationInvokerAdvisor> invokerAdvisors,
			ObjectProvider<EndpointFilter<ExposableWebEndpoint>> filters) {
		return new WebEndpointDiscoverer(this.applicationContext, parameterValueMapper, endpointMediaTypes,
				endpointPathMappers.orderedStream().toList(), invokerAdvisors.orderedStream().toList(),
				filters.orderedStream().toList());
	}

	/**
	 * Creates a {@link ControllerEndpointDiscoverer} bean if no other bean of type
	 * {@link ControllerEndpointsSupplier} is present.
	 * @param endpointPathMappers an {@link ObjectProvider} of {@link PathMapper}
	 * instances used for mapping endpoint paths
	 * @param filters an {@link ObjectProvider} of {@link EndpointFilter} instances used
	 * for filtering controller endpoints
	 * @return a {@link ControllerEndpointDiscoverer} bean
	 */
	@Bean
	@ConditionalOnMissingBean(ControllerEndpointsSupplier.class)
	public ControllerEndpointDiscoverer controllerEndpointDiscoverer(ObjectProvider<PathMapper> endpointPathMappers,
			ObjectProvider<Collection<EndpointFilter<ExposableControllerEndpoint>>> filters) {
		return new ControllerEndpointDiscoverer(this.applicationContext, endpointPathMappers.orderedStream().toList(),
				filters.getIfAvailable(Collections::emptyList));
	}

	/**
	 * Creates a new instance of {@link PathMappedEndpoints} with the specified base path
	 * and endpoint suppliers.
	 * @param endpointSuppliers the collection of endpoint suppliers
	 * @return a new instance of {@link PathMappedEndpoints}
	 */
	@Bean
	@ConditionalOnMissingBean
	public PathMappedEndpoints pathMappedEndpoints(Collection<EndpointsSupplier<?>> endpointSuppliers) {
		return new PathMappedEndpoints(this.properties.getBasePath(), endpointSuppliers);
	}

	/**
	 * Creates a new instance of {@link IncludeExcludeEndpointFilter} for filtering web
	 * endpoints based on inclusion and exclusion patterns.
	 * @return the created {@link IncludeExcludeEndpointFilter} instance
	 */
	@Bean
	public IncludeExcludeEndpointFilter<ExposableWebEndpoint> webExposeExcludePropertyEndpointFilter() {
		WebEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new IncludeExcludeEndpointFilter<>(ExposableWebEndpoint.class, exposure.getInclude(),
				exposure.getExclude(), EndpointExposure.WEB.getDefaultIncludes());
	}

	/**
	 * Creates a new {@link IncludeExcludeEndpointFilter} for
	 * {@link ExposableControllerEndpoint} endpoints based on the configured exposure
	 * properties.
	 * @return The created {@link IncludeExcludeEndpointFilter}.
	 */
	@Bean
	public IncludeExcludeEndpointFilter<ExposableControllerEndpoint> controllerExposeExcludePropertyEndpointFilter() {
		WebEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new IncludeExcludeEndpointFilter<>(ExposableControllerEndpoint.class, exposure.getInclude(),
				exposure.getExclude());
	}

	/**
	 * WebEndpointServletConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class WebEndpointServletConfiguration {

		/**
		 * Creates a new instance of {@link ServletEndpointDiscoverer} if no bean of type
		 * {@link ServletEndpointsSupplier} is present.
		 * @param applicationContext the application context
		 * @param endpointPathMappers the endpoint path mappers
		 * @param filters the endpoint filters
		 * @return the servlet endpoint discoverer
		 */
		@Bean
		@ConditionalOnMissingBean(ServletEndpointsSupplier.class)
		ServletEndpointDiscoverer servletEndpointDiscoverer(ApplicationContext applicationContext,
				ObjectProvider<PathMapper> endpointPathMappers,
				ObjectProvider<EndpointFilter<ExposableServletEndpoint>> filters) {
			return new ServletEndpointDiscoverer(applicationContext, endpointPathMappers.orderedStream().toList(),
					filters.orderedStream().toList());
		}

	}

}
