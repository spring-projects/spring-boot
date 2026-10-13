/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@link OncePerRequestFilter} to add an {@literal X-Application-Context} header that
 * contains the {@link ApplicationContext#getId() ApplicationContext ID}.
 *
 * @author Phillip Webb
 * @author Venil Noronha
 * @since 4.0.0
 */
public class ApplicationContextHeaderFilter extends OncePerRequestFilter {

	/**
	 * Public constant for {@literal X-Application-Context}.
	 */
	public static final String HEADER_NAME = "X-Application-Context";

	private final ApplicationContext applicationContext;

	public ApplicationContextHeaderFilter(ApplicationContext context) {
		this.applicationContext = context;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		response.addHeader(HEADER_NAME, this.applicationContext.getId());
		filterChain.doFilter(request, response);
	}

}
