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

package org.springframework.boot.actuate.endpoint.web.jersey;

import java.util.Collection;
import java.util.HashSet;

import javax.ws.rs.ext.ContextResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.servlet.ServletContainer;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.AbstractWebEndpointIntegrationTests;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Integration tests for web endpoints exposed using Jersey.
 *
 * @author Andy Wilkinson
 * @see JerseyEndpointResourceFactory
 */
public class JerseyWebEndpointIntegrationTests extends
		AbstractWebEndpointIntegrationTests<AnnotationConfigServletWebServerApplicationContext> {

	public JerseyWebEndpointIntegrationTests() {
		super(JerseyConfiguration.class);
	}

	@Override
	protected AnnotationConfigServletWebServerApplicationContext createApplicationContext(
			Class<?>... config) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		context.register(config);
		return context;
	}

	@Override
	protected int getPort(AnnotationConfigServletWebServerApplicationContext context) {
		return context.getWebServer().getPort();
	}

	@Configuration
	static class JerseyConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public ServletRegistrationBean<ServletContainer> servletContainer(
				ResourceConfig resourceConfig) {
			return new ServletRegistrationBean<>(new ServletContainer(resourceConfig),
					"/*");
		}

		@Bean
		public ResourceConfig resourceConfig(Environment environment,
				EndpointDiscoverer<WebOperation> endpointDiscoverer,
				EndpointMediaTypes endpointMediaTypes) {
			ResourceConfig resourceConfig = new ResourceConfig();
			Collection<Resource> resources = new JerseyEndpointResourceFactory()
					.createEndpointResources(
							new EndpointMapping(environment.getProperty("endpointPath")),
							endpointDiscoverer.discoverEndpoints(), endpointMediaTypes);
			resourceConfig.registerResources(new HashSet<>(resources));
			resourceConfig.register(JacksonFeature.class);
			resourceConfig.register(new ObjectMapperContextResolver(new ObjectMapper()),
					ContextResolver.class);
			return resourceConfig;
		}

	}

	private static final class ObjectMapperContextResolver
			implements ContextResolver<ObjectMapper> {

		private final ObjectMapper objectMapper;

		private ObjectMapperContextResolver(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public ObjectMapper getContext(Class<?> type) {
			return this.objectMapper;
		}

	}

}
