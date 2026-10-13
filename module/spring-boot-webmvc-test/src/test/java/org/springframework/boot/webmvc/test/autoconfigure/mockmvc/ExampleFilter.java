/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Example filter used with {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Phillip Webb
 */
@Component
public class ExampleFilter implements Filter, Ordered {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		chain.doFilter(request, response);
		((HttpServletResponse) response).addHeader("x-test", "abc");
	}

	@Override
	public void destroy() {
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
