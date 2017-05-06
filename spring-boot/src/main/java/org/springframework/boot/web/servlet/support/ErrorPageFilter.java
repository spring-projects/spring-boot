/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.servlet.support;

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

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.NestedServletException;

/**
 * A Servlet {@link Filter} that provides an {@link ErrorPageRegistry} for non-embedded
 * applications (i.e. deployed WAR files). It registers error pages and handles
 * application errors by filtering requests and forwarding to the error pages instead of
 * letting the server handle them. Error pages are a feature of the servlet spec but there
 * is no Java API for registering them in the spec. This filter works around that by
 * accepting error page registrations from Spring Boot's {@link ErrorPageRegistrar} (any
 * beans of that type in the context will be applied to this server).
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorPageFilter implements Filter, ErrorPageRegistry {

	private static final Log logger = LogFactory.getLog(ErrorPageFilter.class);

	// From RequestDispatcher but not referenced to remain compatible with Servlet 2.5

	private static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

	private static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

	private static final String ERROR_MESSAGE = "javax.servlet.error.message";

	/**
	 * The name of the servlet attribute containing request URI.
	 */
	public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";

	private static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";

	private String global;

	private final Map<Integer, String> statuses = new HashMap<>();

	private final Map<Class<?>, String> exceptions = new HashMap<>();

	private final OncePerRequestFilter delegate = new OncePerRequestFilter() {

		@Override
		protected void doFilterInternal(HttpServletRequest request,
				HttpServletResponse response, FilterChain chain)
						throws ServletException, IOException {
			ErrorPageFilter.this.doFilter(request, response, chain);
		}

		@Override
		protected boolean shouldNotFilterAsyncDispatch() {
			return false;
		}

	};

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.delegate.init(filterConfig);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		this.delegate.doFilter(request, response, chain);
	}

	private void doFilter(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		ErrorWrapperResponse wrapped = new ErrorWrapperResponse(response);
		try {
			chain.doFilter(request, wrapped);
			if (wrapped.hasErrorToSend()) {
				handleErrorStatus(request, response, wrapped.getStatus(),
						wrapped.getMessage());
				response.flushBuffer();
			}
			else if (!request.isAsyncStarted() && !response.isCommitted()) {
				response.flushBuffer();
			}
		}
		catch (Throwable ex) {
			Throwable exceptionToHandle = ex;
			if (ex instanceof NestedServletException) {
				exceptionToHandle = ((NestedServletException) ex).getRootCause();
			}
			handleException(request, response, wrapped, exceptionToHandle);
			response.flushBuffer();
		}
	}

	private void handleErrorStatus(HttpServletRequest request,
			HttpServletResponse response, int status, String message)
					throws ServletException, IOException {
		if (response.isCommitted()) {
			handleCommittedResponse(request, null);
			return;
		}
		String errorPath = getErrorPath(this.statuses, status);
		if (errorPath == null) {
			response.sendError(status, message);
			return;
		}
		response.setStatus(status);
		setErrorAttributes(request, status, message);
		request.getRequestDispatcher(errorPath).forward(request, response);
	}

	private void handleException(HttpServletRequest request, HttpServletResponse response,
			ErrorWrapperResponse wrapped, Throwable ex)
					throws IOException, ServletException {
		Class<?> type = ex.getClass();
		String errorPath = getErrorPath(type);
		if (errorPath == null) {
			rethrow(ex);
			return;
		}
		if (response.isCommitted()) {
			handleCommittedResponse(request, ex);
			return;
		}

		forwardToErrorPage(errorPath, request, wrapped, ex);
	}

	private void forwardToErrorPage(String path, HttpServletRequest request,
			HttpServletResponse response, Throwable ex)
					throws ServletException, IOException {
		if (logger.isErrorEnabled()) {
			String message = "Forwarding to error page from request "
					+ getDescription(request) + " due to exception [" + ex.getMessage()
					+ "]";
			logger.error(message, ex);
		}
		setErrorAttributes(request, 500, ex.getMessage());
		request.setAttribute(ERROR_EXCEPTION, ex);
		request.setAttribute(ERROR_EXCEPTION_TYPE, ex.getClass());
		response.reset();
		response.sendError(500, ex.getMessage());
		request.getRequestDispatcher(path).forward(request, response);
		request.removeAttribute(ERROR_EXCEPTION);
		request.removeAttribute(ERROR_EXCEPTION_TYPE);
	}

	/**
	 * Return the description for the given request. By default this method will return a
	 * description based on the request {@code servletPath} and {@code pathInfo}.
	 * @param request the source request
	 * @return the description
	 * @since 1.5.0
	 */
	protected String getDescription(HttpServletRequest request) {
		return "[" + request.getServletPath()
				+ (request.getPathInfo() == null ? "" : request.getPathInfo()) + "]";
	}

	private void handleCommittedResponse(HttpServletRequest request, Throwable ex) {
		String message = "Cannot forward to error page for request "
				+ getDescription(request) + " as the response has already been"
				+ " committed. As a result, the response may have the wrong status"
				+ " code. If your application is running on WebSphere Application"
				+ " Server you may be able to resolve this problem by setting"
				+ " com.ibm.ws.webcontainer.invokeFlushAfterService to false";
		if (ex == null) {
			logger.error(message);
		}
		else {
			// User might see the error page without all the data here but throwing the
			// exception isn't going to help anyone (we'll log it to be on the safe side)
			logger.error(message, ex);
		}
	}

	private String getErrorPath(Map<Integer, String> map, Integer status) {
		if (map.containsKey(status)) {
			return map.get(status);
		}
		return this.global;
	}

	private String getErrorPath(Class<?> type) {
		while (type != Object.class) {
			String path = this.exceptions.get(type);
			if (path != null) {
				return path;
			}
			type = type.getSuperclass();
		}
		return this.global;
	}

	private void setErrorAttributes(HttpServletRequest request, int status,
			String message) {
		request.setAttribute(ERROR_STATUS_CODE, status);
		request.setAttribute(ERROR_MESSAGE, message);
		request.setAttribute(ERROR_REQUEST_URI, request.getRequestURI());
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

		private boolean hasErrorToSend = false;

		ErrorWrapperResponse(HttpServletResponse response) {
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
			this.hasErrorToSend = true;
			// Do not call super because the container may prevent us from handling the
			// error ourselves
		}

		@Override
		public int getStatus() {
			if (this.hasErrorToSend) {
				return this.status;
			}
			// If there was no error we need to trust the wrapped response
			return super.getStatus();
		}

		@Override
		public void flushBuffer() throws IOException {
			if (this.hasErrorToSend && !isCommitted()) {
				((HttpServletResponse) getResponse()).sendError(this.status,
						this.message);
			}
			super.flushBuffer();
		}

		public String getMessage() {
			return this.message;
		}

		public boolean hasErrorToSend() {
			return this.hasErrorToSend;
		}

	}

}
