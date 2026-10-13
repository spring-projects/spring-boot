/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The Sample rest controller endpoint with exception.
 *
 * @author Guirong Hu
 */
@Component
@RestControllerEndpoint(id = "exception")
@SuppressWarnings("removal")
public class SampleRestControllerEndpointWithException {

	@GetMapping("/")
	public String exception() {
		throw new CustomException();
	}

	@RestControllerAdvice
	static class CustomExceptionHandler {

		@ExceptionHandler(CustomException.class)
		ResponseEntity<String> handleCustomException(CustomException e) {
			return new ResponseEntity<>("this is a custom exception body", HttpStatus.EXPECTATION_FAILED);
		}

	}

	static class CustomException extends RuntimeException {

	}

}
