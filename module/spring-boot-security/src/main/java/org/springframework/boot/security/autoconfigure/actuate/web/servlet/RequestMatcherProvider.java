/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.actuate.web.servlet;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Interface that can be used to provide a {@link RequestMatcher} that can be used with
 * Spring Security.
 *
 * @author Madhura Bhave
 * @author Chris Bono
 * @since 4.0.0
 */
@FunctionalInterface
public interface RequestMatcherProvider {

	/**
	 * Return the {@link RequestMatcher} to be used for the specified pattern and http
	 * method.
	 * @param pattern the request pattern
	 * @param httpMethod the http method
	 * @return a request matcher
	 */
	RequestMatcher getRequestMatcher(String pattern, @Nullable HttpMethod httpMethod);

}
