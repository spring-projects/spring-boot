/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Details for a welcome page resolved from a resource or a template.
 *
 * @author Phillip Webb
 */
final class WelcomePage {

	/**
	 * Value used for an unresolved welcome page.
	 */
	static final WelcomePage UNRESOLVED = new WelcomePage(null, false);

	private final @Nullable String viewName;

	private final boolean templated;

	private WelcomePage(@Nullable String viewName, boolean templated) {
		this.viewName = viewName;
		this.templated = templated;
	}

	/**
	 * Return the view name of the welcome page.
	 * @return the view name
	 */
	@Nullable String getViewName() {
		return this.viewName;
	}

	/**
	 * Return if the welcome page is from a template.
	 * @return if the welcome page is templated
	 */
	boolean isTemplated() {
		return this.templated;
	}

	/**
	 * Resolve the {@link WelcomePage} to use.
	 * @param templateAvailabilityProviders the template availability providers
	 * @param applicationContext the application context
	 * @param indexHtmlResource the index HTML resource to use or {@code null}
	 * @param staticPathPattern the static path pattern being used
	 * @return a resolved {@link WelcomePage} instance or {@link #UNRESOLVED}
	 */
	static WelcomePage resolve(TemplateAvailabilityProviders templateAvailabilityProviders,
			ApplicationContext applicationContext, @Nullable Resource indexHtmlResource, String staticPathPattern) {
		if (indexHtmlResource != null && "/**".equals(staticPathPattern)) {
			return new WelcomePage("forward:index.html", false);
		}
		if (templateAvailabilityProviders.getProvider("index", applicationContext) != null) {
			return new WelcomePage("index", true);
		}
		return UNRESOLVED;
	}

}
