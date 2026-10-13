/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

/**
 * {@code @ControllerAdvice} annotated {@link ResponseEntityExceptionHandler} that is
 * auto-configured for problem details support.
 *
 * @author Brian Clozel
 */
@ControllerAdvice
class ProblemDetailsExceptionHandler extends ResponseEntityExceptionHandler {

}
