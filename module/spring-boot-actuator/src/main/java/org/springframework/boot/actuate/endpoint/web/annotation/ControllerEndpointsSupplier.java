/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import org.springframework.boot.actuate.endpoint.EndpointsSupplier;

/**
 * {@link EndpointsSupplier} for {@link ExposableControllerEndpoint controller endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 3.3.3 in favor of {@code @Endpoint} and {@code @WebEndpoint} support
 */
@FunctionalInterface
@Deprecated(since = "3.3.3", forRemoval = true)
@SuppressWarnings("removal")
public interface ControllerEndpointsSupplier extends EndpointsSupplier<ExposableControllerEndpoint> {

}
