/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.error;

import org.springframework.web.server.WebExceptionHandler;

/**
 * Marker interface that indicates that a {@link WebExceptionHandler} is used to render
 * errors.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@FunctionalInterface
public interface ErrorWebExceptionHandler extends WebExceptionHandler {

}
