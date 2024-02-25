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
 * <li>errors - Any {@link ObjectError}s from a {@link BindingResult} exception (if
 * configured)</li>
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
 * @since 2.0.0
 * @see ErrorAttributes
 */
public class DefaultErrorAttributes implements ErrorAttributes {

	private static final String ERROR_INTERNAL_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

	/**
     * Retrieves the error attributes for a given server request and error attribute options.
     * 
     * @param request The server request for which to retrieve the error attributes.
     * @param options The error attribute options specifying which attributes to include.
     * @return A map of error attributes.
     */
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

	/**
     * Returns a map of error attributes for the given server request.
     * 
     * @param request The server request for which to retrieve error attributes.
     * @param includeStackTrace Whether to include the stack trace in the error attributes.
     * @return A map of error attributes.
     */
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
		errorAttributes.put("message", determineMessage(error, responseStatusAnnotation));
		errorAttributes.put("requestId", request.exchange().getRequest().getId());
		handleException(errorAttributes, determineException(error), includeStackTrace);
		return errorAttributes;
	}

	/**
     * Determines the HTTP status code to be returned based on the given error and response status annotation.
     * 
     * @param error The throwable error that occurred.
     * @param responseStatusAnnotation The merged annotation containing the response status information.
     * @return The determined HTTP status code.
     */
    private HttpStatus determineHttpStatus(Throwable error, MergedAnnotation<ResponseStatus> responseStatusAnnotation) {
		if (error instanceof ResponseStatusException responseStatusException) {
			HttpStatus httpStatus = HttpStatus.resolve(responseStatusException.getStatusCode().value());
			if (httpStatus != null) {
				return httpStatus;
			}
		}
		return responseStatusAnnotation.getValue("code", HttpStatus.class).orElse(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
     * Determines the appropriate error message based on the given error and response status annotation.
     * 
     * @param error The error object.
     * @param responseStatusAnnotation The response status annotation.
     * @return The determined error message.
     */
    private String determineMessage(Throwable error, MergedAnnotation<ResponseStatus> responseStatusAnnotation) {
		if (error instanceof BindingResult) {
			return error.getMessage();
		}
		if (error instanceof ResponseStatusException responseStatusException) {
			return responseStatusException.getReason();
		}
		String reason = responseStatusAnnotation.getValue("reason", String.class).orElse("");
		if (StringUtils.hasText(reason)) {
			return reason;
		}
		return (error.getMessage() != null) ? error.getMessage() : "";
	}

	/**
     * Determines the exception to be handled.
     * 
     * @param error the error to be determined
     * @return the determined exception
     */
    private Throwable determineException(Throwable error) {
		if (error instanceof ResponseStatusException) {
			return (error.getCause() != null) ? error.getCause() : error;
		}
		return error;
	}

	/**
     * Adds the stack trace of the given error to the error attributes map.
     * 
     * @param errorAttributes the map to store the error attributes
     * @param error the error for which the stack trace needs to be added
     */
    private void addStackTrace(Map<String, Object> errorAttributes, Throwable error) {
		StringWriter stackTrace = new StringWriter();
		error.printStackTrace(new PrintWriter(stackTrace));
		stackTrace.flush();
		errorAttributes.put("trace", stackTrace.toString());
	}

	/**
     * Handles an exception by populating the error attributes map with relevant information.
     * 
     * @param errorAttributes The map to populate with error attributes.
     * @param error The throwable error that occurred.
     * @param includeStackTrace Flag indicating whether to include the stack trace in the error attributes.
     */
    private void handleException(Map<String, Object> errorAttributes, Throwable error, boolean includeStackTrace) {
		errorAttributes.put("exception", error.getClass().getName());
		if (includeStackTrace) {
			addStackTrace(errorAttributes, error);
		}
		if (error instanceof BindingResult result) {
			if (result.hasErrors()) {
				errorAttributes.put("errors", result.getAllErrors());
			}
		}
	}

	/**
     * Retrieves the error associated with the given server request.
     * 
     * @param request the server request
     * @return the error as a Throwable object
     * @throws IllegalStateException if the exception attribute is missing in the ServerWebExchange
     */
    @Override
	public Throwable getError(ServerRequest request) {
		Optional<Object> error = request.attribute(ERROR_INTERNAL_ATTRIBUTE);
		return (Throwable) error
			.orElseThrow(() -> new IllegalStateException("Missing exception attribute in ServerWebExchange"));
	}

	/**
     * Stores the error information in the given ServerWebExchange.
     * 
     * @param error    the Throwable object representing the error
     * @param exchange the ServerWebExchange object to store the error information in
     */
    @Override
	public void storeErrorInformation(Throwable error, ServerWebExchange exchange) {
		exchange.getAttributes().putIfAbsent(ERROR_INTERNAL_ATTRIBUTE, error);
	}

}
