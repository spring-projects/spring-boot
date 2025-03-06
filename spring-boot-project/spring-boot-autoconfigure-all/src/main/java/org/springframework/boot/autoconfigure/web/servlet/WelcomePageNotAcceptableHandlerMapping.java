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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
			ApplicationContext applicationContext, Resource indexHtmlResource, String staticPathPattern) {
		setOrder(LOWEST_PRECEDENCE - 10); // Before ResourceHandlerRegistry
		WelcomePage welcomePage = WelcomePage.resolve(templateAvailabilityProviders, applicationContext,
				indexHtmlResource, staticPathPattern);
		if (welcomePage != WelcomePage.UNRESOLVED) {
			setRootHandler((Controller) this::handleRequest);
		}
	}

	private ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
		return null;
	}

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		return super.getHandlerInternal(request);
	}

}
