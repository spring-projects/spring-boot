/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Abstract base class for error {@link Controller @Controller} implementations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 1.3.0
 * @see ErrorAttributes
 */
public abstract class AbstractErrorController implements ErrorController {

	private final ErrorAttributes errorAttributes;

	private final List<ErrorViewResolver> errorViewResolvers;

	public AbstractErrorController(ErrorAttributes errorAttributes) {
		this(errorAttributes, null);
	}

	public AbstractErrorController(ErrorAttributes errorAttributes, List<ErrorViewResolver> errorViewResolvers) {
		Assert.notNull(errorAttributes, "'errorAttributes' must not be null");
		this.errorAttributes = errorAttributes;
		this.errorViewResolvers = sortErrorViewResolvers(errorViewResolvers);
	}

	private List<ErrorViewResolver> sortErrorViewResolvers(List<ErrorViewResolver> resolvers) {
		List<ErrorViewResolver> sorted = new ArrayList<>();
		if (resolvers != null) {
			sorted.addAll(resolvers);
			AnnotationAwareOrderComparator.sortIfNecessary(sorted);
		}
		return sorted;
	}

	protected Map<String, Object> getErrorAttributes(HttpServletRequest request, ErrorAttributeOptions options) {
		WebRequest webRequest = new ServletWebRequest(request);
		return this.errorAttributes.getErrorAttributes(webRequest, options);
	}

	/**
	 * Returns whether the trace parameter is set.
	 * @param request the request
	 * @return whether the trace parameter is set
	 */
	protected boolean getTraceParameter(HttpServletRequest request) {
		return getBooleanParameter(request, "trace");
	}

	/**
	 * Returns whether the message parameter is set.
	 * @param request the request
	 * @return whether the message parameter is set
	 */
	protected boolean getMessageParameter(HttpServletRequest request) {
		return getBooleanParameter(request, "message");
	}

	/**
	 * Returns whether the errors parameter is set.
	 * @param request the request
	 * @return whether the errors parameter is set
	 */
	protected boolean getErrorsParameter(HttpServletRequest request) {
		return getBooleanParameter(request, "errors");
	}

	/**
	 * Returns whether the path parameter is set.
	 * @param request the request
	 * @return whether the path parameter is set
	 * @since 3.3.0
	 */
	protected boolean getPathParameter(HttpServletRequest request) {
		return getBooleanParameter(request, "path");
	}

	protected boolean getBooleanParameter(HttpServletRequest request, String parameterName) {
		String parameter = request.getParameter(parameterName);
		if (parameter == null) {
			return false;
		}
		return !"false".equalsIgnoreCase(parameter);
	}

	protected HttpStatus getStatus(HttpServletRequest request) {
		Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		if (statusCode == null) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		try {
			return HttpStatus.valueOf(statusCode);
		}
		catch (Exception ex) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
	}

	/**
	 * Resolve any specific error views. By default this method delegates to
	 * {@link ErrorViewResolver ErrorViewResolvers}.
	 * @param request the request
	 * @param response the response
	 * @param status the HTTP status
	 * @param model the suggested model
	 * @return a specific {@link ModelAndView} or {@code null} if the default should be
	 * used
	 * @since 1.4.0
	 */
	protected ModelAndView resolveErrorView(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
			Map<String, Object> model) {
		for (ErrorViewResolver resolver : this.errorViewResolvers) {
			ModelAndView modelAndView = resolver.resolveErrorView(request, status, model);
			if (modelAndView != null) {
				return modelAndView;
			}
		}
		return null;
	}

}
