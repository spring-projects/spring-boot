/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.AbstractConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A special {@link AbstractConfigurableEmbeddedServletContainer} for non-embedded
 * applications (i.e. deployed WAR files). It registers error pages and handles
 * application errors by filtering requests and forwarding to the error pages instead of
 * letting the container handle them. Error pages are a feature of the servlet spec but
 * there is no Java API for registering them in the spec. This filter works around that by
 * accepting error page registrations from Spring Boot's
 * {@link EmbeddedServletContainerCustomizer} (any beans of that type in the context will
 * be applied to this container).
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ErrorPageFilter extends AbstractConfigurableEmbeddedServletContainer implements
		Filter, NonEmbeddedServletContainerFactory {

	private static Log logger = LogFactory.getLog(ErrorPageFilter.class);

	// From RequestDispatcher but not referenced to remain compatible with Servlet 2.5

	private static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

	private static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

	private static final String ERROR_MESSAGE = "javax.servlet.error.message";

	private static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";

	private String global;

	private final Map<Integer, String> statuses = new HashMap<Integer, String>();

	private final Map<Class<?>, String> exceptions = new HashMap<Class<?>, String>();

	private final Map<Class<?>, Class<?>> subtypes = new HashMap<Class<?>, Class<?>>();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest
				&& response instanceof HttpServletResponse) {
			doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
		}
		else {
			chain.doFilter(request, response);
		}
	}

	private void doFilter(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		ErrorWrapperResponse wrapped = new ErrorWrapperResponse(response);
		try {
			chain.doFilter(request, wrapped);
			int status = wrapped.getStatus();
			if (status >= 400) {
				handleErrorStatus(request, response, status, wrapped.getMessage());
			}
		}
		catch (Throwable ex) {
			handleException(request, response, wrapped, ex);
		}
		response.flushBuffer();

	}

	private void handleErrorStatus(HttpServletRequest request,
			HttpServletResponse response, int status, String message)
			throws ServletException, IOException {
		String errorPath = getErrorPath(this.statuses, status);
		if (errorPath == null) {
			response.sendError(status, message);
			return;
		}
		setErrorAttributes(request, status, message);
		request.getRequestDispatcher(errorPath).forward(request, response);
	}

	private void handleException(HttpServletRequest request,
			HttpServletResponse response, ErrorWrapperResponse wrapped, Throwable ex)
			throws IOException, ServletException {
		Class<?> type = ex.getClass();
		String errorPath = getErrorPath(type);
		if (errorPath == null) {
			rethrow(ex);
			return;
		}
		setErrorAttributes(request, 500, ex.getMessage());
		request.setAttribute(ERROR_EXCEPTION, ex);
		request.setAttribute(ERROR_EXCEPTION_TYPE, type.getName());
		forwardToErrorPage(errorPath, request, wrapped, ex);
	}

	private void forwardToErrorPage(String path, HttpServletRequest request,
			HttpServletResponse response, Throwable ex) throws ServletException,
			IOException {
		if (response.isCommitted()) {
			String message = "Cannot forward to error page for" + request.getRequestURI()
					+ " (response is committed), so this response may have "
					+ "the wrong status code";
			// User might see the error page without all the data here but throwing the
			// exception isn't going to help anyone (we'll log it to be on the safe side)
			logger.error(message, ex);
			return;
		}
		response.reset();
		response.sendError(500, ex.getMessage());
		request.getRequestDispatcher(path).forward(request, response);
	}

	private String getErrorPath(Map<Integer, String> map, Integer status) {
		if (map.containsKey(status)) {
			return map.get(status);
		}
		return this.global;
	}

	private String getErrorPath(Class<?> type) {
		if (this.exceptions.containsKey(type)) {
			return this.exceptions.get(type);
		}
		if (this.subtypes.containsKey(type)) {
			return this.exceptions.get(this.subtypes.get(type));
		}
		Class<?> subtype = type;
		while (subtype != Object.class) {
			subtype = subtype.getSuperclass();
			if (this.exceptions.containsKey(subtype)) {
				this.subtypes.put(subtype, type);
				return this.exceptions.get(subtype);
			}
		}
		return this.global;
	}

	private void setErrorAttributes(ServletRequest request, int status, String message) {
		request.setAttribute(ERROR_STATUS_CODE, status);
		request.setAttribute(ERROR_MESSAGE, message);
	}

	private void rethrow(Throwable ex) throws IOException, ServletException {
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		if (ex instanceof IOException) {
			throw (IOException) ex;
		}
		if (ex instanceof ServletException) {
			throw (ServletException) ex;
		}
		throw new IllegalStateException(ex);
	}

	@Override
	public void addErrorPages(ErrorPage... errorPages) {
		for (ErrorPage errorPage : errorPages) {
			if (errorPage.isGlobal()) {
				this.global = errorPage.getPath();
			}
			else if (errorPage.getStatus() != null) {
				this.statuses.put(errorPage.getStatus().value(), errorPage.getPath());
			}
			else {
				this.exceptions.put(errorPage.getException(), errorPage.getPath());
			}
		}
	}

	@Override
	public void destroy() {
	}

	private static class ErrorWrapperResponse extends HttpServletResponseWrapper {

		private int status;

		private String message;

		public ErrorWrapperResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void sendError(int status) throws IOException {
			sendError(status, null);
		}

		@Override
		public void sendError(int status, String message) throws IOException {
			this.status = status;
			this.message = message;
		}

		@Override
		public int getStatus() {
			return this.status;
		}

		public String getMessage() {
			return this.message;
		}

	}

}
