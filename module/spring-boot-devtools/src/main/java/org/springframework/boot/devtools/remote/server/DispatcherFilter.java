/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.remote.server;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Servlet filter providing integration with the remote server {@link Dispatcher}.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 */
public class DispatcherFilter implements Filter {

	private final Dispatcher dispatcher;

	public DispatcherFilter(Dispatcher dispatcher) {
		Assert.notNull(dispatcher, "'dispatcher' must not be null");
		this.dispatcher = dispatcher;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest httpServletRequest
				&& response instanceof HttpServletResponse httpServletResponse) {
			doFilter(httpServletRequest, httpServletResponse, chain);
		}
		else {
			chain.doFilter(request, response);
		}
	}

	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		ServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
		ServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
		if (!this.dispatcher.handle(serverRequest, serverResponse)) {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
	}

}
