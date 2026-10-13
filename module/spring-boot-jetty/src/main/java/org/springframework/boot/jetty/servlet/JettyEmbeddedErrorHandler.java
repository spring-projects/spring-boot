/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.servlet;

import org.eclipse.jetty.ee11.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.http.HttpMethod;

/**
 * Variation of Jetty's {@link ErrorPageErrorHandler} that supports all {@link HttpMethod
 * HttpMethods} rather than just {@code GET}, {@code POST} and {@code HEAD}. By default
 * Jetty <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=446039">intentionally only
 * supports a limited set of HTTP methods</a> for error pages, however, Spring Boot
 * prefers Tomcat and Jetty to all behave in the same way.
 *
 * @author Phillip Webb
 * @author Christoph Dreis
 */
class JettyEmbeddedErrorHandler extends ErrorPageErrorHandler {

	@Override
	public boolean errorPageForMethod(String method) {
		return true;
	}

}
