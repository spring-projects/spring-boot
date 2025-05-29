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

package org.springframework.boot.webmvc.actuate.endpoint.web.test;

import java.util.Collections;

import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.webmvc.actuate.endpoint.web.WebMvcEndpointHandlerMapping;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Endpoint configuration for WebMvc.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
		WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class })
class WebMvcEndpointConfiguration {

	private final ApplicationContext applicationContext;

	WebMvcEndpointConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	TomcatServletWebServerFactory tomcat() {
		return new TomcatServletWebServerFactory(0);
	}

	@Bean
	WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping() {
		EndpointMediaTypes endpointMediaTypes = EndpointMediaTypes.DEFAULT;
		WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(this.applicationContext,
				new ConversionServiceParameterValueMapper(), endpointMediaTypes, Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		return new WebMvcEndpointHandlerMapping(new EndpointMapping("/actuator"), discoverer.getEndpoints(),
				endpointMediaTypes, new CorsConfiguration(), new EndpointLinksResolver(discoverer.getEndpoints()),
				true);
	}

}
