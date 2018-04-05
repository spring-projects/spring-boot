/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeStacktrace;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * Basic global error {@link Controller}, rendering {@link ErrorAttributes}. More specific
 * errors can be handled either using Spring MVC abstractions (e.g.
 * {@code @ExceptionHandler}) or by adding servlet
 * {@link AbstractServletWebServerFactory#setErrorPages server error pages}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Stephane Nicoll
 * @see ErrorAttributes
 * @see ErrorProperties
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class BasicErrorController extends AbstractErrorController {

	private final ErrorProperties errorProperties;

	/**
	 * Create a new {@link BasicErrorController} instance.
	 * @param errorAttributes the error attributes
	 * @param errorProperties configuration properties
	 */
	public BasicErrorController(ErrorAttributes errorAttributes,
			ErrorProperties errorProperties) {
		this(errorAttributes, errorProperties, Collections.emptyList());
	}

	/**
	 * Create a new {@link BasicErrorController} instance.
	 * @param errorAttributes the error attributes
	 * @param errorProperties configuration properties
	 * @param errorViewResolvers error view resolvers
	 */
	public BasicErrorController(ErrorAttributes errorAttributes,
			ErrorProperties errorProperties, List<ErrorViewResolver> errorViewResolvers) {
		super(errorAttributes, errorViewResolvers);
		Assert.notNull(errorProperties, "ErrorProperties must not be null");
		this.errorProperties = errorProperties;
	}

	@Override
	public String getErrorPath() {
		return this.errorProperties.getPath();
	}

	@RequestMapping(produces = "text/html")
	public ModelAndView errorHtml(HttpServletRequest request,
			HttpServletResponse response) {
		HttpStatus status = getStatus(request);
		Map<String, Object> model = Collections.unmodifiableMap(getErrorAttributes(
				request, isIncludeStackTrace(request, MediaType.TEXT_HTML)));
		response.setStatus(status.value());
		ModelAndView modelAndView = resolveErrorView(request, response, status, model);
		return (modelAndView == null ? new ModelAndView("error", model) : modelAndView);
	}

	@RequestMapping
	@ResponseBody
	public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
		Map<String, Object> body = getErrorAttributes(request,
				isIncludeStackTrace(request, MediaType.ALL));
		HttpStatus status = getStatus(request);
		return new ResponseEntity<>(body, status);
	}

	/**
	 * Determine if the stacktrace attribute should be included.
	 * @param request the source request
	 * @param produces the media type produced (or {@code MediaType.ALL})
	 * @return if the stacktrace attribute should be included
	 */
	protected boolean isIncludeStackTrace(HttpServletRequest request,
			MediaType produces) {
		IncludeStacktrace include = getErrorProperties().getIncludeStacktrace();
		if (include == IncludeStacktrace.ALWAYS) {
			return true;
		}
		if (include == IncludeStacktrace.ON_TRACE_PARAM) {
			return getTraceParameter(request);
		}
		return false;
	}

	/**
	 * Provide access to the error properties.
	 * @return the error properties
	 */
	protected ErrorProperties getErrorProperties() {
		return this.errorProperties;
	}

}
