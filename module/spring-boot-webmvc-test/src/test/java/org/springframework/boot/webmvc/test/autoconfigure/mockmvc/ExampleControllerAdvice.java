/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Example {@link ControllerAdvice @ControllerAdvice} used with
 * {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@ControllerAdvice
public class ExampleControllerAdvice {

	@ExceptionHandler
	public ResponseEntity<String> onExampleError(ExampleException exception) {
		return ResponseEntity.ok("recovered");
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<String> noHandlerFoundHandler(NoHandlerFoundException exception) {
		return ResponseEntity.badRequest().body("Invalid request: " + exception.getRequestURL());
	}

}
