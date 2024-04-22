/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.reactive.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default implementation of {@link ErrorAttributes}. Provides the following attributes
 * when possible:
 * <ul>
 * <li>timestamp - The time that the errors were extracted</li>
 * <li>status - The status code</li>
 * <li>error - The error reason</li>
 * <li>exception - The class name of the root exception (if configured)</li>
 * <li>message - The exception message (if configured)</li>
 * <li>errors - Any {@link ObjectError}s from a {@link BindingResult} or
 * {@link MethodValidationResult} exception (if configured)</li>
 * <li>trace - The exception stack trace (if configured)</li>
 * <li>path - The URL path when the exception was raised</li>
 * <li>requestId - Unique ID associated with the current request</li>
 * </ul>
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Michele Mancioppi
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Yanming Zhou
 * @since 2.0.0
 * @see ErrorAttributes
 */
public class DefaultErrorAttributes implements ErrorAttributes {

	private static final String ERROR_INTERNAL_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

	@Override
	public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
		Map<String, Object> errorAttributes = getErrorAttributes(request, options.isIncluded(Include.STACK_TRACE));
		if (!options.isIncluded(Include.EXCEPTION)) {
			errorAttributes.remove("exception");
		}
		if (!options.isIncluded(Include.STACK_TRACE)) {
			errorAttributes.remove("trace");
		}
		if (!options.isIncluded(Include.MESSAGE) && errorAttributes.get("message") != null) {
			errorAttributes.remove("message");
		}
		if (!options.isIncluded(Include.BINDING_ERRORS)) {
			errorAttributes.remove("errors");
		}
		if (!options.isIncluded(Include.PATH)) {
			errorAttributes.remove("path");
		}
		return errorAttributes;
	}

	private Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
		Map<String, Object> errorAttributes = new LinkedHashMap<>();
		errorAttributes.put("timestamp", new Date());
		errorAttributes.put("path", request.requestPath().value());
		Throwable error = getError(request);
		MergedAnnotation<ResponseStatus> responseStatusAnnotation = MergedAnnotations
			.from(error.getClass(), SearchStrategy.TYPE_HIERARCHY)
			.get(ResponseStatus.class);
		HttpStatus errorStatus = determineHttpStatus(error, responseStatusAnnotation);
		errorAttributes.put("status", errorStatus.value());
		errorAttributes.put("error", errorStatus.getReasonPhrase());
		errorAttributes.put("requestId", request.exchange().getRequest().getId());
		handleException(errorAttributes, error, responseStatusAnnotation, includeStackTrace);
		return errorAttributes;
	}

	private HttpStatus determineHttpStatus(Throwable error, MergedAnnotation<ResponseStatus> responseStatusAnnotation) {
		if (error instanceof ResponseStatusException responseStatusException) {
			HttpStatus httpStatus = HttpStatus.resolve(responseStatusException.getStatusCode().value());
			if (httpStatus != null) {
				return httpStatus;
			}
		}
		return responseStatusAnnotation.getValue("code", HttpStatus.class).orElse(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private void addStackTrace(Map<String, Object> errorAttributes, Throwable error) {
		StringWriter stackTrace = new StringWriter();
		error.printStackTrace(new PrintWriter(stackTrace));
		stackTrace.flush();
		errorAttributes.put("trace", stackTrace.toString());
	}

	private void handleException(Map<String, Object> errorAttributes, Throwable error,
			MergedAnnotation<ResponseStatus> responseStatusAnnotation, boolean includeStackTrace) {
		Throwable exception;
		if (error instanceof BindingResult bindingResult) {
			errorAttributes.put("message", error.getMessage());
			errorAttributes.put("errors", bindingResult.getAllErrors());
			exception = error;
		}
		else if (error instanceof MethodValidationResult methodValidationResult) {
			addMessageAndErrorsFromMethodValidationResult(errorAttributes, methodValidationResult);
			exception = error;
		}
		else if (error instanceof ResponseStatusException responseStatusException) {
			errorAttributes.put("message", responseStatusException.getReason());
			exception = (responseStatusException.getCause() != null) ? responseStatusException.getCause() : error;
		}
		else {
			exception = error;
			String reason = responseStatusAnnotation.getValue("reason", String.class).orElse("");
			String message = StringUtils.hasText(reason) ? reason : error.getMessage();
			errorAttributes.put("message", (message != null) ? message : "");
		}
		errorAttributes.put("exception", exception.getClass().getName());
		if (includeStackTrace) {
			addStackTrace(errorAttributes, exception);
		}
	}

	private void addMessageAndErrorsFromMethodValidationResult(Map<String, Object> errorAttributes,
			MethodValidationResult result) {
		List<ObjectError> errors = result.getAllErrors()
			.stream()
			.filter(ObjectError.class::isInstance)
			.map(ObjectError.class::cast)
			.toList();
		errorAttributes.put("message",
				"Validation failed for method='" + result.getMethod() + "'. Error count: " + errors.size());
		errorAttributes.put("errors", errors);
	}

	@Override
	public Throwable getError(ServerRequest request) {
		Optional<Object> error = request.attribute(ERROR_INTERNAL_ATTRIBUTE);
		return (Throwable) error
			.orElseThrow(() -> new IllegalStateException("Missing exception attribute in ServerWebExchange"));
	}

	@Override
	public void storeErrorInformation(Throwable error, ServerWebExchange exchange) {
		exchange.getAttributes().putIfAbsent(ERROR_INTERNAL_ATTRIBUTE, error);
	}

}
