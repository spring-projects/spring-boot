/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A special {@link EmbeddedServletContainerFactory} for non-embedded applications (i.e.
 * deployed WAR files). It registers error pages and handles application errors by
 * filtering requests and forwarding to the error pages instead of letting the container
 * handle them. Error pages are a feature of the servlet spec but there is no Java API for
 * registering them in the spec. This filter works around that by accepting error page
 * registrations from Spring Boot's {@link EmbeddedServletContainerCustomizer} (any beans
 * of that type in the context will be applied to this container).
 * 
 * @author Dave Syer
 * 
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorWrapperEmbeddedServletContainerFactory extends
		AbstractEmbeddedServletContainerFactory implements Filter {

	private String global;

	private Map<Integer, String> statuses = new HashMap<Integer, String>();

	private Map<Class<? extends Throwable>, String> exceptions = new HashMap<Class<? extends Throwable>, String>();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		String errorPath;
		ErrorWrapperResponse wrapped = new ErrorWrapperResponse(
				(HttpServletResponse) response);
		try {
			chain.doFilter(request, wrapped);
			int status = wrapped.getStatus();
			if (status >= 400) {
				errorPath = this.statuses.containsKey(status) ? this.statuses.get(status)
						: this.global;
				if (errorPath != null) {
					request.setAttribute("javax.servlet.error.status_code", status);
					request.setAttribute("javax.servlet.error.message",
							wrapped.getMessage());
					((HttpServletRequest) request).getRequestDispatcher(errorPath)
							.forward(request, response);
				}
				else {
					((HttpServletResponse) response).sendError(status,
							wrapped.getMessage());
				}
			}
		}
		catch (Throwable e) {
			Class<? extends Throwable> cls = e.getClass();
			errorPath = this.exceptions.containsKey(cls) ? this.exceptions.get(cls)
					: this.global;
			if (errorPath != null) {
				request.setAttribute("javax.servlet.error.status_code", 500);
				request.setAttribute("javax.servlet.error.exception", e);
				request.setAttribute("javax.servlet.error.message", e.getMessage());
				wrapped.sendError(500, e.getMessage());
				((HttpServletRequest) request).getRequestDispatcher(errorPath).forward(
						request, response);
			}
			else {
				rethrow(e);
			}
		}
	}

	private void rethrow(Throwable e) throws IOException, ServletException {
		if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		}
		if (e instanceof Error) {
			throw (Error) e;
		}
		if (e instanceof IOException) {
			throw (IOException) e;
		}
		if (e instanceof ServletException) {
			throw (ServletException) e;
		}
		throw new IllegalStateException("Unidentified Exception", e);
	}

	@Override
	public EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers) {
		return new EmbeddedServletContainer() {

			@Override
			public void start() throws EmbeddedServletContainerException {
			}

			@Override
			public void stop() throws EmbeddedServletContainerException {
			}

			@Override
			public int getPort() {
				return -1;
			}
		};
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
