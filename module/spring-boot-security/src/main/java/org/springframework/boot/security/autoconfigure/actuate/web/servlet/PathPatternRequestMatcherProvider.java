/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.actuate.web.servlet;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * {@link RequestMatcherProvider} that provides an {@link PathPatternRequestMatcher}.
 *
 * @author Madhura Bhave
 * @author Chris Bono
 */
class PathPatternRequestMatcherProvider implements RequestMatcherProvider {

	private final Function<String, String> pathFactory;

	PathPatternRequestMatcherProvider(Function<String, String> pathFactory) {
		this.pathFactory = pathFactory;
	}

	@Override
	public RequestMatcher getRequestMatcher(String pattern, @Nullable HttpMethod httpMethod) {
		return PathPatternRequestMatcher.withDefaults().matcher(httpMethod, this.pathFactory.apply(pattern));
	}

}
