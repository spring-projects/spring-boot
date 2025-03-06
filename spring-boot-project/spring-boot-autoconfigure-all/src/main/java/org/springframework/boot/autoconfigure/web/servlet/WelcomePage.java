/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.servlet;

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

	private final String viewName;

	private final boolean templated;

	private WelcomePage(String viewName, boolean templated) {
		this.viewName = viewName;
		this.templated = templated;
	}

	/**
	 * Return the view name of the welcome page.
	 * @return the view name
	 */
	String getViewName() {
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
			ApplicationContext applicationContext, Resource indexHtmlResource, String staticPathPattern) {
		if (indexHtmlResource != null && "/**".equals(staticPathPattern)) {
			return new WelcomePage("forward:index.html", false);
		}
		if (templateAvailabilityProviders.getProvider("index", applicationContext) != null) {
			return new WelcomePage("index", true);
		}
		return UNRESOLVED;
	}

}
