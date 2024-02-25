/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.servlet.support;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.web.filter.OncePerRequestFilter;

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
public class ErrorPageFilter implements Filter, ErrorPageRegistry, Ordered {

	private static final Log logger = LogFactory.getLog(ErrorPageFilter.class);

	// From RequestDispatcher but not referenced to remain compatible with Servlet 2.5

	private static final String ERROR_EXCEPTION = "jakarta.servlet.error.exception";

	private static final String ERROR_EXCEPTION_TYPE = "jakarta.servlet.error.exception_type";

	private static final String ERROR_MESSAGE = "jakarta.servlet.error.message";

	/**
	 * The name of the servlet attribute containing request URI.
	 */
	public static final String ERROR_REQUEST_URI = "jakarta.servlet.error.request_uri";

	private static final String ERROR_STATUS_CODE = "jakarta.servlet.error.status_code";

	private static final Set<Class<?>> CLIENT_ABORT_EXCEPTIONS;
	static {
		Set<Class<?>> clientAbortExceptions = new HashSet<>();
		addClassIfPresent(clientAbortExceptions, "org.apache.catalina.connector.ClientAbortException");
		CLIENT_ABORT_EXCEPTIONS = Collections.unmodifiableSet(clientAbortExceptions);
	}

	private String global;

	private final Map<Integer, String> statuses = new HashMap<>();

	private final Map<Class<?>, String> exceptions = new HashMap<>();

	private final OncePerRequestFilter delegate = new OncePerRequestFilter() {

		/**
		 * This method is called by the servlet container each time a request/response
		 * pair is passed through the chain due to a client request for a resource at the
		 * end of the chain. It delegates the request/response pair to the next filter in
		 * the chain or to the servlet if the end of the chain is reached.
		 * @param request the HttpServletRequest object that contains the client's request
		 * @param response the HttpServletResponse object that contains the servlet's
		 * response
		 * @param chain the FilterChain object that allows the filter to pass on the
		 * request and response to the next filter in the chain
		 * @throws ServletException if an exception occurs that interferes with the
		 * filter's normal operation
		 * @throws IOException if an I/O related error occurs during the processing of the
		 * request
		 */
		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {
			ErrorPageFilter.this.doFilter(request, response, chain);
		}

		/**
		 * Determines whether the filter should not be applied to asynchronous dispatches.
		 * @return {@code false} indicating that the filter should be applied to
		 * asynchronous dispatches.
		 */
		@Override
		protected boolean shouldNotFilterAsyncDispatch() {
			return false;
		}

	};

	/**
	 * Initializes the filter by calling the init method of the delegate filter.
	 * @param filterConfig the filter configuration object
	 * @throws ServletException if an error occurs during initialization
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.delegate.init(filterConfig);
	}

	/**
	 * This method is responsible for filtering the servlet request and response. It
	 * delegates the filtering process to the delegate object.
	 * @param request the servlet request to be filtered
	 * @param response the servlet response to be filtered
	 * @param chain the filter chain to be used for further filtering
	 * @throws IOException if an I/O error occurs during the filtering process
	 * @throws ServletException if a servlet-specific error occurs during the filtering
	 * process
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		this.delegate.doFilter(request, response, chain);
	}

	/**
	 * Filters the incoming request and response, handling any errors that occur.
	 * @param request the HttpServletRequest object representing the incoming request
	 * @param response the HttpServletResponse object representing the outgoing response
	 * @param chain the FilterChain object for invoking the next filter in the chain
	 * @throws IOException if an I/O error occurs during the filtering process
	 * @throws ServletException if a servlet error occurs during the filtering process
	 */
	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		ErrorWrapperResponse wrapped = new ErrorWrapperResponse(response);
		try {
			chain.doFilter(request, wrapped);
			if (wrapped.hasErrorToSend()) {
				handleErrorStatus(request, response, wrapped.getStatus(), wrapped.getMessage());
				response.flushBuffer();
			}
			else if (!request.isAsyncStarted() && !response.isCommitted()) {
				response.flushBuffer();
			}
		}
		catch (Throwable ex) {
			Throwable exceptionToHandle = ex;
			if (ex instanceof ServletException servletException) {
				Throwable rootCause = servletException.getRootCause();
				if (rootCause != null) {
					exceptionToHandle = rootCause;
				}
			}
			handleException(request, response, wrapped, exceptionToHandle);
			response.flushBuffer();
		}
	}

	/**
	 * Handles the error status by sending an appropriate response or forwarding the
	 * request to an error page.
	 * @param request the HttpServletRequest object representing the client's request
	 * @param response the HttpServletResponse object representing the server's response
	 * @param status the HTTP status code of the error
	 * @param message the error message to be sent or displayed
	 * @throws ServletException if an error occurs while handling the request
	 * @throws IOException if an I/O error occurs while handling the request
	 */
	private void handleErrorStatus(HttpServletRequest request, HttpServletResponse response, int status, String message)
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

	/**
	 * Handles exceptions thrown during the processing of an error page.
	 * @param request the HttpServletRequest object representing the current request
	 * @param response the HttpServletResponse object representing the current response
	 * @param wrapped the ErrorWrapperResponse object containing the wrapped error
	 * response
	 * @param ex the Throwable object representing the exception that was thrown
	 * @throws IOException if an I/O error occurs while handling the exception
	 * @throws ServletException if a servlet error occurs while handling the exception
	 */
	private void handleException(HttpServletRequest request, HttpServletResponse response, ErrorWrapperResponse wrapped,
			Throwable ex) throws IOException, ServletException {
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

	/**
	 * Forwards the request to the error page with the specified path, sets the error
	 * attributes, and handles the exception.
	 * @param path the path of the error page
	 * @param request the HttpServletRequest object
	 * @param response the HttpServletResponse object
	 * @param ex the Throwable object representing the exception
	 * @throws ServletException if there is an error while forwarding the request
	 * @throws IOException if there is an error while handling the request or response
	 */
	private void forwardToErrorPage(String path, HttpServletRequest request, HttpServletResponse response, Throwable ex)
			throws ServletException, IOException {
		if (logger.isErrorEnabled()) {
			String message = "Forwarding to error page from request " + getDescription(request) + " due to exception ["
					+ ex.getMessage() + "]";
			logger.error(message, ex);
		}
		setErrorAttributes(request, 500, ex.getMessage());
		request.setAttribute(ERROR_EXCEPTION, ex);
		request.setAttribute(ERROR_EXCEPTION_TYPE, ex.getClass());
		response.reset();
		response.setStatus(500);
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
		String pathInfo = (request.getPathInfo() != null) ? request.getPathInfo() : "";
		return "[" + request.getServletPath() + pathInfo + "]";
	}

	/**
	 * Handles the committed response for the given HttpServletRequest and Throwable. If
	 * the Throwable is a ClientAbortException, the method returns without further
	 * processing. Otherwise, it logs an error message indicating that the response has
	 * already been committed, and suggests a possible solution for WebSphere Application
	 * Server. If the Throwable is not null, it logs the error message along with the
	 * Throwable.
	 * @param request The HttpServletRequest object representing the current request.
	 * @param ex The Throwable object representing the exception that occurred.
	 */
	private void handleCommittedResponse(HttpServletRequest request, Throwable ex) {
		if (isClientAbortException(ex)) {
			return;
		}
		String message = "Cannot forward to error page for request " + getDescription(request)
				+ " as the response has already been"
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

	/**
	 * Checks if the given Throwable is a ClientAbortException or any of its subclasses.
	 * @param ex the Throwable to check
	 * @return true if the Throwable is a ClientAbortException or any of its subclasses,
	 * false otherwise
	 */
	private boolean isClientAbortException(Throwable ex) {
		if (ex == null) {
			return false;
		}
		for (Class<?> candidate : CLIENT_ABORT_EXCEPTIONS) {
			if (candidate.isInstance(ex)) {
				return true;
			}
		}
		return isClientAbortException(ex.getCause());
	}

	/**
	 * Returns the error path based on the given status code.
	 * @param map the map containing the status code and corresponding error path
	 * @param status the status code for which the error path is to be retrieved
	 * @return the error path corresponding to the given status code, or the global error
	 * path if not found in the map
	 */
	private String getErrorPath(Map<Integer, String> map, Integer status) {
		if (map.containsKey(status)) {
			return map.get(status);
		}
		return this.global;
	}

	/**
	 * Returns the error path for the given exception type.
	 * @param type the exception type
	 * @return the error path for the exception type
	 */
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

	/**
	 * Sets the error attributes for the given request.
	 * @param request the HttpServletRequest object
	 * @param status the status code of the error
	 * @param message the error message
	 */
	private void setErrorAttributes(HttpServletRequest request, int status, String message) {
		request.setAttribute(ERROR_STATUS_CODE, status);
		request.setAttribute(ERROR_MESSAGE, message);
		request.setAttribute(ERROR_REQUEST_URI, request.getRequestURI());
	}

	/**
	 * Rethrows the given Throwable, wrapping it in an appropriate exception if necessary.
	 * @param ex the Throwable to be rethrown
	 * @throws IOException if the Throwable is an instance of IOException
	 * @throws ServletException if the Throwable is an instance of ServletException
	 * @throws RuntimeException if the Throwable is an instance of RuntimeException
	 * @throws Error if the Throwable is an instance of Error
	 * @throws IllegalStateException if the Throwable is not an instance of any of the
	 * above exceptions
	 */
	private void rethrow(Throwable ex) throws IOException, ServletException {
		if (ex instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		if (ex instanceof Error error) {
			throw error;
		}
		if (ex instanceof IOException ioException) {
			throw ioException;
		}
		if (ex instanceof ServletException servletException) {
			throw servletException;
		}
		throw new IllegalStateException(ex);
	}

	/**
	 * Adds error pages to the ErrorPageFilter.
	 * @param errorPages the error pages to be added
	 */
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

	/**
	 * This method is called by the web container to indicate to a filter that it is being
	 * taken out of service. It is called after all filter methods have been completed for
	 * a request. This method gives the filter an opportunity to clean up any resources
	 * that it is holding (for example, memory, file handles, threads) and make sure that
	 * any persistent state is synchronized with the filter's current state in memory.
	 * This method is called only once all threads within the filter's doFilter method
	 * have exited or after a timeout period has passed.
	 */
	@Override
	public void destroy() {
	}

	/**
	 * Returns the order of this filter in the filter chain. The order is set to be one
	 * higher than the highest precedence, ensuring that this filter is executed first.
	 * @return the order of this filter
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	/**
	 * Adds a class to the given collection if it is present.
	 * @param collection the collection to add the class to
	 * @param className the name of the class to add
	 */
	private static void addClassIfPresent(Collection<Class<?>> collection, String className) {
		try {
			collection.add(ClassUtils.forName(className, null));
		}
		catch (Throwable ex) {
			// Ignore
		}
	}

	/**
	 * ErrorWrapperResponse class.
	 */
	private static class ErrorWrapperResponse extends HttpServletResponseWrapper {

		private int status;

		private String message;

		private boolean hasErrorToSend = false;

		/**
		 * Constructs a new ErrorWrapperResponse object with the specified
		 * HttpServletResponse.
		 * @param response the HttpServletResponse object to be wrapped
		 */
		ErrorWrapperResponse(HttpServletResponse response) {
			super(response);
		}

		/**
		 * Sends an error response to the client using the specified HTTP status code.
		 * @param status the HTTP status code to send
		 * @throws IOException if an I/O error occurs while sending the error response
		 */
		@Override
		public void sendError(int status) throws IOException {
			sendError(status, null);
		}

		/**
		 * Sends an error response with the specified status code and message.
		 * @param status the status code of the error response
		 * @param message the error message to be sent
		 * @throws IOException if an I/O error occurs while sending the error response
		 */
		@Override
		public void sendError(int status, String message) throws IOException {
			this.status = status;
			this.message = message;
			this.hasErrorToSend = true;
			// Do not call super because the container may prevent us from handling the
			// error ourselves
		}

		/**
		 * Returns the status code of the response. If there is an error to send, the
		 * status code of the error is returned. Otherwise, the status code of the wrapped
		 * response is returned.
		 * @return the status code of the response
		 */
		@Override
		public int getStatus() {
			if (this.hasErrorToSend) {
				return this.status;
			}
			// If there was no error we need to trust the wrapped response
			return super.getStatus();
		}

		/**
		 * Flushes the buffer and sends any necessary error response before flushing.
		 * @throws IOException if an I/O error occurs
		 */
		@Override
		public void flushBuffer() throws IOException {
			sendErrorIfNecessary();
			super.flushBuffer();
		}

		/**
		 * Sends an error response if necessary.
		 * @throws IOException if an I/O error occurs while sending the error response
		 */
		private void sendErrorIfNecessary() throws IOException {
			if (this.hasErrorToSend && !isCommitted()) {
				((HttpServletResponse) getResponse()).sendError(this.status, this.message);
			}
		}

		/**
		 * Returns the message of the ErrorWrapperResponse object.
		 * @return the message of the ErrorWrapperResponse object
		 */
		String getMessage() {
			return this.message;
		}

		/**
		 * Returns a boolean value indicating whether there is an error to send.
		 * @return true if there is an error to send, false otherwise
		 */
		boolean hasErrorToSend() {
			return this.hasErrorToSend;
		}

		/**
		 * Returns a PrintWriter object that can send character text to the client.
		 * @return a PrintWriter object that can send character text to the client
		 * @throws IOException if an I/O error occurs
		 */
		@Override
		public PrintWriter getWriter() throws IOException {
			sendErrorIfNecessary();
			return super.getWriter();
		}

		/**
		 * Returns the output stream for writing binary data to the client.
		 * @return the output stream for writing binary data to the client
		 * @throws IOException if an I/O error occurs
		 */
		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			sendErrorIfNecessary();
			return super.getOutputStream();
		}

	}

}
