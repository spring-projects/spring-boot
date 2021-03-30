/*
 * Copyright 2012-2021 the original author or authors.
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

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
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
		Assert.notNull(errorAttributes, "ErrorAttributes must not be null");
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

	/**
	 * Returns a {@link Map} of the error attributes.
	 * @param request the source request
	 * @param includeStackTrace if stack trace elements should be included
	 * @return the error attributes
	 * @deprecated since 2.3.0 for removal in 2.5.0 in favor of
	 * {@link #getErrorAttributes(HttpServletRequest, ErrorAttributeOptions)}
	 */
	@Deprecated
	protected Map<String, Object> getErrorAttributes(HttpServletRequest request, boolean includeStackTrace) {
		return getErrorAttributes(request,
				(includeStackTrace) ? ErrorAttributeOptions.of(Include.STACK_TRACE) : ErrorAttributeOptions.defaults());
	}

	protected Map<String, Object> getErrorAttributes(HttpServletRequest request, ErrorAttributeOptions options) {
		WebRequest webRequest = new ServletWebRequest(request);
		return this.errorAttributes.getErrorAttributes(webRequest, options);
	}

	protected boolean getTraceParameter(HttpServletRequest request) {
		return getBooleanParameter(request, "trace");
	}

	protected boolean getMessageParameter(HttpServletRequest request) {
		return getBooleanParameter(request, "message");
	}

	protected boolean getErrorsParameter(HttpServletRequest request) {
		return getBooleanParameter(request, "errors");
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
