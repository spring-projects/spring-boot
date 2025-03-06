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

package org.springframework.boot.autoconfigure.web.reactive;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * A {@link RouterFunction} factory for an application's welcome page. Supports both
 * static and templated files. If both a static and templated index page are available,
 * the static page is preferred.
 *
 * @author Brian Clozel
 */
final class WelcomePageRouterFunctionFactory {

	private final String staticPathPattern;

	private final Resource welcomePage;

	private final boolean welcomePageTemplateExists;

	WelcomePageRouterFunctionFactory(TemplateAvailabilityProviders templateAvailabilityProviders,
			ApplicationContext applicationContext, String[] staticLocations, String staticPathPattern) {
		this.staticPathPattern = staticPathPattern;
		this.welcomePage = getWelcomePage(applicationContext, staticLocations);
		this.welcomePageTemplateExists = welcomeTemplateExists(templateAvailabilityProviders, applicationContext);
	}

	private Resource getWelcomePage(ResourceLoader resourceLoader, String[] staticLocations) {
		return Arrays.stream(staticLocations)
			.map((location) -> getIndexHtml(resourceLoader, location))
			.filter(this::isReadable)
			.findFirst()
			.orElse(null);
	}

	private Resource getIndexHtml(ResourceLoader resourceLoader, String location) {
		return resourceLoader.getResource(location + "index.html");
	}

	private boolean isReadable(Resource resource) {
		try {
			return resource.exists() && (resource.getURL() != null);
		}
		catch (Exception ex) {
			return false;
		}
	}

	private boolean welcomeTemplateExists(TemplateAvailabilityProviders templateAvailabilityProviders,
			ApplicationContext applicationContext) {
		return templateAvailabilityProviders.getProvider("index", applicationContext) != null;
	}

	RouterFunction<ServerResponse> createRouterFunction() {
		if (this.welcomePage != null && "/**".equals(this.staticPathPattern)) {
			return RouterFunctions.route(GET("/").and(accept(MediaType.TEXT_HTML)),
					(req) -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(this.welcomePage));
		}
		else if (this.welcomePageTemplateExists) {
			return RouterFunctions.route(GET("/").and(accept(MediaType.TEXT_HTML)),
					(req) -> ServerResponse.ok().render("index"));
		}
		return null;
	}

}
