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
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;

/**
 * {@link BlockJUnit4ClassRunner} for Spring MVC.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class WebMvcEndpointRunner extends AbstractWebEndpointRunner {

	WebMvcEndpointRunner(Class<?> testClass) throws InitializationError {
		super(testClass, "Spring MVC", WebMvcEndpointRunner::createContext);
	}

	private static ConfigurableApplicationContext createContext(List<Class<?>> classes) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		classes.add(WebMvcEndpointConfiguration.class);
		context.register(classes.toArray(new Class<?>[classes.size()]));
		context.refresh();
		return context;
	}

	@Configuration
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class })
	static class WebMvcEndpointConfiguration {

		private final ApplicationContext applicationContext;

		WebMvcEndpointConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping() {
			List<String> mediaTypes = Arrays.asList(MediaType.APPLICATION_JSON_VALUE,
					ActuatorMediaType.V2_JSON);
			EndpointMediaTypes endpointMediaTypes = new EndpointMediaTypes(mediaTypes,
					mediaTypes);
			WebAnnotationEndpointDiscoverer discoverer = new WebAnnotationEndpointDiscoverer(
					this.applicationContext, new ConversionServiceParameterMapper(),
					endpointMediaTypes, EndpointPathResolver.useEndpointId(), null, null);
			return new WebMvcEndpointHandlerMapping(new EndpointMapping("/application"),
					discoverer.discoverEndpoints(), endpointMediaTypes,
					new CorsConfiguration());
		}

	}

}
