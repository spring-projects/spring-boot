/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.web.servlet;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Simple example Filter used for testing.
 *
 * @author Phillip Webb
 */
public class ExampleFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		response.getWriter().write("[");
		chain.doFilter(request, response);
		response.getWriter().write("]");
	}

}
