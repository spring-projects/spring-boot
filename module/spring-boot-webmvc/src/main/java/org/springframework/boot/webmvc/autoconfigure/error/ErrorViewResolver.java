/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.error;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interface that can be implemented by beans that resolve error views.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface ErrorViewResolver {

	/**
	 * Resolve an error view for the specified details.
	 * @param request the source request
	 * @param status the http status of the error
	 * @param model the suggested model to be used with the view
	 * @return a resolved {@link ModelAndView} or {@code null}
	 */
	@Nullable ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model);

}
