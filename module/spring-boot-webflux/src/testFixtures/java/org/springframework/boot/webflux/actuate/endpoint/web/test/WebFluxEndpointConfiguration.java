/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.endpoint.web.test;

import java.util.Collections;

import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.webflux.actuate.endpoint.web.WebFluxEndpointHandlerMapping;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Endpoint configuration for WebFlux.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, WebFluxAutoConfiguration.class })
class WebFluxEndpointConfiguration {

	private final ApplicationContext applicationContext;

	WebFluxEndpointConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	NettyReactiveWebServerFactory netty() {
		return new NettyReactiveWebServerFactory(0);
	}

	@Bean
	HttpHandler httpHandler(ApplicationContext applicationContext) {
		return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
	}

	@Bean
	WebFluxEndpointHandlerMapping webEndpointReactiveHandlerMapping() {
		EndpointMediaTypes endpointMediaTypes = EndpointMediaTypes.DEFAULT;
		WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(this.applicationContext,
				new ConversionServiceParameterValueMapper(), endpointMediaTypes, Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		return new WebFluxEndpointHandlerMapping(new EndpointMapping("/actuator"), discoverer.getEndpoints(),
				endpointMediaTypes, new CorsConfiguration(), new EndpointLinksResolver(discoverer.getEndpoints()),
				true);
	}

}
