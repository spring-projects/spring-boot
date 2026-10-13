/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;

/**
 * An {@link AbstractUrlHandlerMapping} for an application's welcome page that was
 * ultimately not accepted.
 *
 * @author Phillip Webb
 */
class WelcomePageNotAcceptableHandlerMapping extends AbstractUrlHandlerMapping {

	WelcomePageNotAcceptableHandlerMapping(TemplateAvailabilityProviders templateAvailabilityProviders,
			ApplicationContext applicationContext, @Nullable Resource indexHtmlResource, String staticPathPattern) {
		setOrder(LOWEST_PRECEDENCE - 10); // Before ResourceHandlerRegistry
		WelcomePage welcomePage = WelcomePage.resolve(templateAvailabilityProviders, applicationContext,
				indexHtmlResource, staticPathPattern);
		if (welcomePage != WelcomePage.UNRESOLVED) {
			setRootHandler((Controller) this::handleRequest);
		}
	}

	private @Nullable ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
		return null;
	}

	@Override
	protected @Nullable Object getHandlerInternal(HttpServletRequest request) throws Exception {
		return super.getHandlerInternal(request);
	}

}
