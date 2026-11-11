/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.actuate.endpoint.web.test;

import java.util.List;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointInfrastructureProvider;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest.Infrastructure;

/**
 * {@link WebEndpointInfrastructureProvider} for MVC.
 *
 * @author Stephane Nicoll
 */
class WebMvcWebEndpointInfrastructureProvider implements WebEndpointInfrastructureProvider {

	@Override
	public boolean supports(Infrastructure infrastructure) {
		return infrastructure == Infrastructure.MVC;
	}

	@Override
	public List<Class<?>> getInfrastructureConfiguration(Infrastructure infrastructure) {
		return List.of(WebMvcEndpointConfiguration.class);
	}

}
