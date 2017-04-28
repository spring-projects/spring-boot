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

package org.springframework.boot.web.embedded.jetty;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

/**
 * Variation of Jetty's {@link ErrorHandler} that supports all {@link HttpMethod
 * HttpMethods} rather than just {@code GET}, {@code POST} and {@code HEAD}. Jetty
 * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=446039">intentionally only
 * supports a limited set of HTTP methods</a> for error pages, however, Spring Boot
 * prefers Tomcat, Jetty and Undertow to all behave in the same way.
 *
 * @author Phillip Webb
 */
class JettyEmbeddedErrorHandler extends ErrorHandler {

	private final ErrorHandler delegate;

	JettyEmbeddedErrorHandler(ErrorHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String method = request.getMethod();
		if (!HttpMethod.GET.is(method) && !HttpMethod.POST.is(method)
				&& !HttpMethod.HEAD.is(method)) {
			request = new ErrorHttpServletRequest(request);
		}
		this.delegate.handle(target, baseRequest, request, response);
	}

	private static class ErrorHttpServletRequest extends HttpServletRequestWrapper {

		private boolean simulateGetMethod = true;

		ErrorHttpServletRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getMethod() {
			return (this.simulateGetMethod ? HttpMethod.GET.toString()
					: super.getMethod());
		}

		@Override
		public ServletContext getServletContext() {
			this.simulateGetMethod = false;
			return super.getServletContext();
		}

	}

}
