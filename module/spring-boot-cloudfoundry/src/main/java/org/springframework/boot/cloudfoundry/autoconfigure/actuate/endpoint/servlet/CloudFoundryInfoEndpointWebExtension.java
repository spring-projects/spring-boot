/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.servlet;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.EndpointCloudFoundryExtension;

/**
 * {@link EndpointExtension @EndpointExtension} for the {@link InfoEndpoint} that always
 * exposes full git details.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@EndpointCloudFoundryExtension(endpoint = InfoEndpoint.class)
public class CloudFoundryInfoEndpointWebExtension {

	private final InfoEndpoint delegate;

	public CloudFoundryInfoEndpointWebExtension(InfoEndpoint delegate) {
		this.delegate = delegate;
	}

	@ReadOperation
	public Map<String, Object> info() {
		return this.delegate.info();
	}

}
