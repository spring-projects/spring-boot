/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.extension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;

@Configuration(proxyBeanMethods = false)
public class MyExtensionConfiguration {

	@Bean
	public MyExtensionWebMvcEndpointHandlerMapping myWebMvcEndpointHandlerMapping(
			WebEndpointsSupplier webEndpointsSupplier, EndpointMediaTypes endpointMediaTypes,
			ObjectProvider<CorsEndpointProperties> corsPropertiesProvider, WebEndpointProperties webEndpointProperties,
			Environment environment, ApplicationContext applicationContext, ParameterValueMapper parameterMapper) {
		CorsEndpointProperties corsProperties = corsPropertiesProvider.getIfAvailable();
		CorsConfiguration corsConfiguration = (corsProperties != null) ? corsProperties.toCorsConfiguration() : null;
		List<OperationInvokerAdvisor> invokerAdvisors = Collections.emptyList();
		List<EndpointFilter<ExposableWebEndpoint>> filters = Collections
			.singletonList(new MyExtensionEndpointFilter(environment));
		WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(applicationContext, parameterMapper,
				endpointMediaTypes, null, null, invokerAdvisors, filters, Collections.emptyList());
		Collection<ExposableWebEndpoint> endpoints = discoverer.getEndpoints();
		return new MyExtensionWebMvcEndpointHandlerMapping(endpoints, endpointMediaTypes, corsConfiguration);
	}

}
