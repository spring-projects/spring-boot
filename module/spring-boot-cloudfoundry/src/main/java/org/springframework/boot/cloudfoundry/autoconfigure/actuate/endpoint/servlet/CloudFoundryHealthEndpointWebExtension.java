/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.servlet;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.EndpointCloudFoundryExtension;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointWebExtension;

/**
 * {@link EndpointExtension @EndpointExtension} for the {@link HealthEndpoint} that always
 * exposes full health details.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@EndpointCloudFoundryExtension(endpoint = HealthEndpoint.class)
public class CloudFoundryHealthEndpointWebExtension {

	private final HealthEndpointWebExtension delegate;

	public CloudFoundryHealthEndpointWebExtension(HealthEndpointWebExtension delegate) {
		this.delegate = delegate;
	}

	@ReadOperation
	public WebEndpointResponse<HealthDescriptor> health(ApiVersion apiVersion) {
		return this.delegate.health(apiVersion, null, SecurityContext.NONE, true);
	}

	@ReadOperation
	public WebEndpointResponse<HealthDescriptor> health(ApiVersion apiVersion,
			@Selector(match = Match.ALL_REMAINING) String... path) {
		return this.delegate.health(apiVersion, null, SecurityContext.NONE, true, path);
	}

}
