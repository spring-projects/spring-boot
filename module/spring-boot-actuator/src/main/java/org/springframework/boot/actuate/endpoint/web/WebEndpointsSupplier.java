/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.boot.actuate.endpoint.EndpointsSupplier;

/**
 * {@link EndpointsSupplier} for {@link ExposableWebEndpoint web endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface WebEndpointsSupplier extends EndpointsSupplier<ExposableWebEndpoint> {

}
