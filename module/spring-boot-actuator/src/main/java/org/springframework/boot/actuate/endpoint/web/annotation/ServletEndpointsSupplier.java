/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;

/**
 * {@link EndpointsSupplier} for {@link ExposableServletEndpoint servlet endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 3.3.0 in favor of {@code @Endpoint} and {@code @WebEndpoint}
 */
@FunctionalInterface
@Deprecated(since = "3.3.0", forRemoval = true)
@SuppressWarnings("removal")
public interface ServletEndpointsSupplier extends EndpointsSupplier<ExposableServletEndpoint> {

}
