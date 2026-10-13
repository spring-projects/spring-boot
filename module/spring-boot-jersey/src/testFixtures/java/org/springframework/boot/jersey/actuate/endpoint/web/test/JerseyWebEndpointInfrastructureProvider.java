/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.actuate.endpoint.web.test;

import java.util.List;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointInfrastructureProvider;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest.Infrastructure;

/**
 * {@link WebEndpointInfrastructureProvider} for Jersey.
 *
 * @author Stephane Nicoll
 */
class JerseyWebEndpointInfrastructureProvider implements WebEndpointInfrastructureProvider {

	@Override
	public boolean supports(Infrastructure infrastructure) {
		return infrastructure == Infrastructure.JERSEY;
	}

	@Override
	public List<Class<?>> getInfrastructureConfiguration(Infrastructure infrastructure) {
		return List.of(JerseyEndpointConfiguration.class);
	}

}
