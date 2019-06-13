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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.ExposeExcludePropertyEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@AutoConfigureAfter(EndpointAutoConfiguration.class)
@EnableConfigurationProperties(WebEndpointProperties.class)
public class WebEndpointAutoConfiguration {

	private static final List<String> MEDIA_TYPES = Arrays.asList(ActuatorMediaType.V2_JSON, "application/json");

	private final ApplicationContext applicationContext;

	private final WebEndpointProperties properties;

	public WebEndpointAutoConfiguration(ApplicationContext applicationContext, WebEndpointProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@Bean
	public PathMapper webEndpointPathMapper() {
		return new MappingWebEndpointPathMapper(this.properties.getPathMapping());
	}

	@Bean
	@ConditionalOnMissingBean
	public EndpointMediaTypes endpointMediaTypes() {
		return new EndpointMediaTypes(MEDIA_TYPES, MEDIA_TYPES);
	}

	@Bean
	@ConditionalOnMissingBean(WebEndpointsSupplier.class)
	public WebEndpointDiscoverer webEndpointDiscoverer(ParameterValueMapper parameterValueMapper,
			EndpointMediaTypes endpointMediaTypes, ObjectProvider<PathMapper> endpointPathMappers,
			ObjectProvider<OperationInvokerAdvisor> invokerAdvisors,
			ObjectProvider<EndpointFilter<ExposableWebEndpoint>> filters) {
		return new WebEndpointDiscoverer(this.applicationContext, parameterValueMapper, endpointMediaTypes,
				endpointPathMappers.orderedStream().collect(Collectors.toList()),
				invokerAdvisors.orderedStream().collect(Collectors.toList()),
				filters.orderedStream().collect(Collectors.toList()));
	}

	@Bean
	@ConditionalOnMissingBean(ControllerEndpointsSupplier.class)
	public ControllerEndpointDiscoverer controllerEndpointDiscoverer(ObjectProvider<PathMapper> endpointPathMappers,
			ObjectProvider<Collection<EndpointFilter<ExposableControllerEndpoint>>> filters) {
		return new ControllerEndpointDiscoverer(this.applicationContext,
				endpointPathMappers.orderedStream().collect(Collectors.toList()),
				filters.getIfAvailable(Collections::emptyList));
	}

	@Bean
	@ConditionalOnMissingBean
	public PathMappedEndpoints pathMappedEndpoints(Collection<EndpointsSupplier<?>> endpointSuppliers) {
		return new PathMappedEndpoints(this.properties.getBasePath(), endpointSuppliers);
	}

	@Bean
	public ExposeExcludePropertyEndpointFilter<ExposableWebEndpoint> webExposeExcludePropertyEndpointFilter() {
		WebEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new ExposeExcludePropertyEndpointFilter<>(ExposableWebEndpoint.class, exposure.getInclude(),
				exposure.getExclude(), "info", "health");
	}

	@Bean
	public ExposeExcludePropertyEndpointFilter<ExposableControllerEndpoint> controllerExposeExcludePropertyEndpointFilter() {
		WebEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new ExposeExcludePropertyEndpointFilter<>(ExposableControllerEndpoint.class, exposure.getInclude(),
				exposure.getExclude());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class WebEndpointServletConfiguration {

		@Bean
		@ConditionalOnMissingBean(ServletEndpointsSupplier.class)
		public ServletEndpointDiscoverer servletEndpointDiscoverer(ApplicationContext applicationContext,
				ObjectProvider<PathMapper> endpointPathMappers,
				ObjectProvider<EndpointFilter<ExposableServletEndpoint>> filters) {
			return new ServletEndpointDiscoverer(applicationContext,
					endpointPathMappers.orderedStream().collect(Collectors.toList()),
					filters.orderedStream().collect(Collectors.toList()));
		}

	}

}
