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

package org.springframework.boot.web.servlet.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

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
 * </ul>
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 2.0.0
 * @see ErrorAttributes
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultErrorAttributes implements ErrorAttributes, HandlerExceptionResolver, Ordered {

	private static final String ERROR_INTERNAL_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

	/**
	 * Returns the order of this DefaultErrorAttributes instance. The order is set to the
	 * highest precedence value.
	 * @return the order value
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	/**
	 * Resolves the exception thrown during the execution of a handler method.
	 * @param request the HttpServletRequest object representing the current request
	 * @param response the HttpServletResponse object representing the current response
	 * @param handler the handler object that was executed
	 * @param ex the exception that was thrown during the execution of the handler method
	 * @return null
	 */
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		storeErrorAttributes(request, ex);
		return null;
	}

	/**
	 * Stores the error attributes and exception in the request attribute.
	 * @param request the HttpServletRequest object
	 * @param ex the Exception object
	 */
	private void storeErrorAttributes(HttpServletRequest request, Exception ex) {
		request.setAttribute(ERROR_INTERNAL_ATTRIBUTE, ex);
	}

	/**
	 * Returns the error attributes for the given web request and error attribute options.
	 * @param webRequest The web request for which to retrieve the error attributes.
	 * @param options The error attribute options specifying which attributes to include.
	 * @return A map of error attributes.
	 */
	@Override
	public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
		Map<String, Object> errorAttributes = getErrorAttributes(webRequest, options.isIncluded(Include.STACK_TRACE));
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
	 * Returns a map of error attributes for the given web request.
	 * @param webRequest the web request
	 * @param includeStackTrace whether to include the stack trace in the error details
	 * @return a map of error attributes
	 */
	private Map<String, Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace) {
		Map<String, Object> errorAttributes = new LinkedHashMap<>();
		errorAttributes.put("timestamp", new Date());
		addStatus(errorAttributes, webRequest);
		addErrorDetails(errorAttributes, webRequest, includeStackTrace);
		addPath(errorAttributes, webRequest);
		return errorAttributes;
	}

	/**
	 * Adds the status and error attributes to the errorAttributes map. If the status
	 * attribute is not found in the requestAttributes, it sets the status to 999 and
	 * error to "None". Otherwise, it sets the status to the value found in the
	 * requestAttributes and tries to obtain the corresponding reason phrase from the
	 * HttpStatus enum. If the reason phrase is not found, it sets the error to "Http
	 * Status " + status.
	 * @param errorAttributes the map to which the status and error attributes will be
	 * added
	 * @param requestAttributes the request attributes from which the status attribute
	 * will be retrieved
	 */
	private void addStatus(Map<String, Object> errorAttributes, RequestAttributes requestAttributes) {
		Integer status = getAttribute(requestAttributes, RequestDispatcher.ERROR_STATUS_CODE);
		if (status == null) {
			errorAttributes.put("status", 999);
			errorAttributes.put("error", "None");
			return;
		}
		errorAttributes.put("status", status);
		try {
			errorAttributes.put("error", HttpStatus.valueOf(status).getReasonPhrase());
		}
		catch (Exception ex) {
			// Unable to obtain a reason
			errorAttributes.put("error", "Http Status " + status);
		}
	}

	/**
	 * Adds error details to the given error attributes map.
	 * @param errorAttributes the map to add error details to
	 * @param webRequest the current web request
	 * @param includeStackTrace whether to include the stack trace in the error details
	 */
	private void addErrorDetails(Map<String, Object> errorAttributes, WebRequest webRequest,
			boolean includeStackTrace) {
		Throwable error = getError(webRequest);
		if (error != null) {
			while (error instanceof ServletException && error.getCause() != null) {
				error = error.getCause();
			}
			errorAttributes.put("exception", error.getClass().getName());
			if (includeStackTrace) {
				addStackTrace(errorAttributes, error);
			}
		}
		addErrorMessage(errorAttributes, webRequest, error);
	}

	/**
	 * Adds an error message to the given error attributes map based on the provided error
	 * and request. If the error is a binding result, the error message is extracted from
	 * the binding result. Otherwise, the error message is extracted from the exception.
	 * @param errorAttributes the error attributes map to add the error message to
	 * @param webRequest the web request associated with the error
	 * @param error the error or exception to extract the error message from
	 */
	private void addErrorMessage(Map<String, Object> errorAttributes, WebRequest webRequest, Throwable error) {
		BindingResult result = extractBindingResult(error);
		if (result == null) {
			addExceptionErrorMessage(errorAttributes, webRequest, error);
		}
		else {
			addBindingResultErrorMessage(errorAttributes, result);
		}
	}

	/**
	 * Adds the error message to the error attributes map.
	 * @param errorAttributes the error attributes map
	 * @param webRequest the web request
	 * @param error the throwable error
	 */
	private void addExceptionErrorMessage(Map<String, Object> errorAttributes, WebRequest webRequest, Throwable error) {
		errorAttributes.put("message", getMessage(webRequest, error));
	}

	/**
	 * Returns the message to be included as the value of the {@code message} error
	 * attribute. By default the returned message is the first of the following that is
	 * not empty:
	 * <ol>
	 * <li>Value of the {@link RequestDispatcher#ERROR_MESSAGE} request attribute.
	 * <li>Message of the given {@code error}.
	 * <li>{@code No message available}.
	 * </ol>
	 * @param webRequest current request
	 * @param error current error, if any
	 * @return message to include in the error attributes
	 * @since 2.4.0
	 */
	protected String getMessage(WebRequest webRequest, Throwable error) {
		Object message = getAttribute(webRequest, RequestDispatcher.ERROR_MESSAGE);
		if (!ObjectUtils.isEmpty(message)) {
			return message.toString();
		}
		if (error != null && StringUtils.hasLength(error.getMessage())) {
			return error.getMessage();
		}
		return "No message available";
	}

	/**
	 * Adds an error message and errors to the given error attributes map based on the
	 * provided BindingResult.
	 * @param errorAttributes the error attributes map to add the error message and errors
	 * to
	 * @param result the BindingResult containing the validation errors
	 */
	private void addBindingResultErrorMessage(Map<String, Object> errorAttributes, BindingResult result) {
		errorAttributes.put("message", "Validation failed for object='" + result.getObjectName() + "'. "
				+ "Error count: " + result.getErrorCount());
		errorAttributes.put("errors", result.getAllErrors());
	}

	/**
	 * Extracts the BindingResult from the given Throwable error.
	 * @param error the Throwable error to extract the BindingResult from
	 * @return the BindingResult if the error is an instance of BindingResult, otherwise
	 * null
	 */
	private BindingResult extractBindingResult(Throwable error) {
		if (error instanceof BindingResult bindingResult) {
			return bindingResult;
		}
		return null;
	}

	/**
	 * Adds the stack trace of the given error to the error attributes map.
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
	 * Adds the path attribute to the error attributes map.
	 * @param errorAttributes the error attributes map
	 * @param requestAttributes the request attributes
	 */
	private void addPath(Map<String, Object> errorAttributes, RequestAttributes requestAttributes) {
		String path = getAttribute(requestAttributes, RequestDispatcher.ERROR_REQUEST_URI);
		if (path != null) {
			errorAttributes.put("path", path);
		}
	}

	/**
	 * Retrieves the error that occurred during the processing of the web request.
	 * @param webRequest the current web request
	 * @return the Throwable object representing the error, or null if no error occurred
	 */
	@Override
	public Throwable getError(WebRequest webRequest) {
		Throwable exception = getAttribute(webRequest, ERROR_INTERNAL_ATTRIBUTE);
		if (exception == null) {
			exception = getAttribute(webRequest, RequestDispatcher.ERROR_EXCEPTION);
		}
		return exception;
	}

	/**
	 * Retrieves the attribute with the specified name from the given RequestAttributes
	 * object.
	 * @param requestAttributes the RequestAttributes object from which to retrieve the
	 * attribute
	 * @param name the name of the attribute to retrieve
	 * @return the attribute value, or null if the attribute does not exist
	 * @throws ClassCastException if the attribute value cannot be cast to the specified
	 * type
	 * @param <T> the type of the attribute value
	 *
	 * @since 1.0
	 */
	@SuppressWarnings("unchecked")
	private <T> T getAttribute(RequestAttributes requestAttributes, String name) {
		return (T) requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
	}

}
