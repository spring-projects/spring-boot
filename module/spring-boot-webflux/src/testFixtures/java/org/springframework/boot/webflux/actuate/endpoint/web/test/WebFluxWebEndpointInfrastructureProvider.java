/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.endpoint.web.test;

import java.util.List;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointInfrastructureProvider;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest.Infrastructure;

/**
 * {@link WebEndpointInfrastructureProvider} for WebFlux.
 *
 * @author Stephane Nicoll
 */
class WebFluxWebEndpointInfrastructureProvider implements WebEndpointInfrastructureProvider {

	@Override
	public boolean supports(Infrastructure infrastructure) {
		return infrastructure == Infrastructure.WEBFLUX;
	}

	@Override
	public List<Class<?>> getInfrastructureConfiguration(Infrastructure infrastructure) {
		return List.of(WebFluxEndpointConfiguration.class);
	}

}
