/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.actuate.endpoint.web;

import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Strategy interface used to provide the remaining path segments for a Jersey actuator
 * endpoint.
 *
 * @author Madhura Bhave
 */
interface JerseyRemainingPathSegmentProvider {

	String get(ContainerRequestContext requestContext, String matchAllRemainingPathSegmentsVariable);

}
