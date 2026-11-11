/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.test.autoconfigure.webmvc;

import java.io.IOException;
import java.security.Principal;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.Ordered;

/**
 * {@link Filter} that is ordered to run after Spring Security's filter.
 *
 * @author Andy Wilkinson
 */
public class AfterSecurityFilter implements Filter, Ordered {

	@Override
	public int getOrder() {
		return SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Principal principal = ((HttpServletRequest) request).getUserPrincipal();
		if (principal == null) {
			throw new ServletException("No user principal");
		}
		response.getWriter().write(principal.getName());
		response.getWriter().flush();
	}

}
