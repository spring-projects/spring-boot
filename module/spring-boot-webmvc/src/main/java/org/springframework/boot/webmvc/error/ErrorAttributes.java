/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.error;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides access to error attributes which can be logged or presented to the user.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0.0
 * @see DefaultErrorAttributes
 */
public interface ErrorAttributes {

	/**
	 * Returns a {@link Map} of the error attributes. The map can be used as the model of
	 * an error page {@link ModelAndView}, or returned as a
	 * {@link ResponseBody @ResponseBody}.
	 * @param webRequest the source request
	 * @param options options for error attribute contents
	 * @return a map of error attributes
	 */
	default Map<String, @Nullable Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
		return Collections.emptyMap();
	}

	/**
	 * Return the underlying cause of the error or {@code null} if the error cannot be
	 * extracted.
	 * @param webRequest the source request
	 * @return the {@link Exception} that caused the error or {@code null}
	 */
	@Nullable Throwable getError(WebRequest webRequest);

}
