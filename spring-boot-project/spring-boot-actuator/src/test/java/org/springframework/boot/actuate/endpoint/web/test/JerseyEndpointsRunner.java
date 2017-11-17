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

package org.springframework.boot.actuate.endpoint.web.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.jersey.JerseyEndpointResourceFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link BlockJUnit4ClassRunner} for Jersey.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class JerseyEndpointsRunner extends AbstractWebEndpointRunner {

	JerseyEndpointsRunner(Class<?> testClass) throws InitializationError {
		super(testClass, "Jersey", JerseyEndpointsRunner::createContext);
	}

	private static ConfigurableApplicationContext createContext(List<Class<?>> classes) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		classes.add(JerseyEndpointConfiguration.class);
		context.register(classes.toArray(new Class<?>[classes.size()]));
		context.refresh();
		return context;
	}

	@Configuration
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			JerseyAutoConfiguration.class })
	static class JerseyEndpointConfiguration {

		private final ApplicationContext applicationContext;

		JerseyEndpointConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}

		@Bean
		public ResourceConfigCustomizer webEndpointRegistrar() {
			return this::customize;
		}

		private void customize(ResourceConfig config) {
			List<String> mediaTypes = Arrays.asList(MediaType.APPLICATION_JSON,
					ActuatorMediaType.V2_JSON);
			EndpointMediaTypes endpointMediaTypes = new EndpointMediaTypes(mediaTypes,
					mediaTypes);
			WebAnnotationEndpointDiscoverer discoverer = new WebAnnotationEndpointDiscoverer(
					this.applicationContext, new ConversionServiceParameterMapper(),
					endpointMediaTypes, EndpointPathResolver.useEndpointId(), null, null);
			Collection<Resource> resources = new JerseyEndpointResourceFactory()
					.createEndpointResources(new EndpointMapping("/application"),
							discoverer.discoverEndpoints(), endpointMediaTypes);
			config.registerResources(new HashSet<>(resources));
		}

	}

}
