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
import org.springframework.boot.actuate.endpoint.web.reactive.WebFluxEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * {@link BlockJUnit4ClassRunner} for Spring WebFlux.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class WebFluxEndpointsRunner extends AbstractWebEndpointRunner {

	WebFluxEndpointsRunner(Class<?> testClass) throws InitializationError {
		super(testClass, "Reactive", WebFluxEndpointsRunner::createContext);
	}

	private static ConfigurableApplicationContext createContext(List<Class<?>> classes) {
		AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();
		classes.add(WebFluxEndpointConfiguration.class);
		context.register(classes.toArray(new Class<?>[classes.size()]));
		context.refresh();
		return context;
	}

	@Configuration
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			WebFluxAutoConfiguration.class })
	static class WebFluxEndpointConfiguration
			implements ApplicationListener<WebServerInitializedEvent> {

		private final ApplicationContext applicationContext;

		WebFluxEndpointConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		public NettyReactiveWebServerFactory netty() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		public PortHolder portHolder() {
			return new PortHolder();
		}

		@Override
		public void onApplicationEvent(WebServerInitializedEvent event) {
			portHolder().setPort(event.getWebServer().getPort());
		}

		@Bean
		public HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		public WebFluxEndpointHandlerMapping webEndpointReactiveHandlerMapping() {
			List<String> mediaTypes = Arrays.asList(MediaType.APPLICATION_JSON_VALUE,
					ActuatorMediaType.V2_JSON);
			EndpointMediaTypes endpointMediaTypes = new EndpointMediaTypes(mediaTypes,
					mediaTypes);
			WebAnnotationEndpointDiscoverer discoverer = new WebAnnotationEndpointDiscoverer(
					this.applicationContext, new ConversionServiceParameterMapper(),
					endpointMediaTypes, EndpointPathResolver.useEndpointId(), null, null);
			return new WebFluxEndpointHandlerMapping(new EndpointMapping("/application"),
					discoverer.discoverEndpoints(), endpointMediaTypes,
					new CorsConfiguration());
		}

	}

}
