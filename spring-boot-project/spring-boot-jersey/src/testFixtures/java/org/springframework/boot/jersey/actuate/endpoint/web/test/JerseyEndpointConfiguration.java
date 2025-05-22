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

package org.springframework.boot.jersey.actuate.endpoint.web.test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jersey.actuate.endpoint.web.JerseyEndpointResourceFactory;
import org.springframework.boot.jersey.autoconfigure.JerseyAutoConfiguration;
import org.springframework.boot.jersey.autoconfigure.ResourceConfigCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Endpoint configuration for Jersey.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, JerseyAutoConfiguration.class })
class JerseyEndpointConfiguration {

	private final ApplicationContext applicationContext;

	JerseyEndpointConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	TomcatServletWebServerFactory tomcat() {
		return new TomcatServletWebServerFactory(0);
	}

	@Bean
	ResourceConfig resourceConfig() {
		return new ResourceConfig();
	}

	@Bean
	ResourceConfigCustomizer webEndpointRegistrar() {
		return this::customize;
	}

	private void customize(ResourceConfig config) {
		EndpointMediaTypes endpointMediaTypes = EndpointMediaTypes.DEFAULT;
		WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(this.applicationContext,
				new ConversionServiceParameterValueMapper(), endpointMediaTypes, null, Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		Collection<Resource> resources = new JerseyEndpointResourceFactory().createEndpointResources(
				new EndpointMapping("/actuator"), discoverer.getEndpoints(), endpointMediaTypes,
				new EndpointLinksResolver(discoverer.getEndpoints()), true);
		config.registerResources(new HashSet<>(resources));
	}

}
