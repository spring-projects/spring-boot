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

import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * An {@link AbstractUrlHandlerMapping} for an application's HTML welcome page. Supports
 * both static and templated files. If both a static and templated index page are
 * available, the static page is preferred.
 *
 * @author Andy Wilkinson
 * @author Bruce Brouwer
 * @author Moritz Halbritter
 * @see WelcomePageNotAcceptableHandlerMapping
 */
final class WelcomePageHandlerMapping extends AbstractUrlHandlerMapping {

	private static final Log logger = LogFactory.getLog(WelcomePageHandlerMapping.class);

	private static final List<MediaType> MEDIA_TYPES_ALL = Collections.singletonList(MediaType.ALL);

	WelcomePageHandlerMapping(TemplateAvailabilityProviders templateAvailabilityProviders,
			ApplicationContext applicationContext, Resource indexHtmlResource, String staticPathPattern) {
		setOrder(2);
		WelcomePage welcomePage = WelcomePage.resolve(templateAvailabilityProviders, applicationContext,
				indexHtmlResource, staticPathPattern);
		if (welcomePage != WelcomePage.UNRESOLVED) {
			logger.info(LogMessage.of(() -> (!welcomePage.isTemplated()) ? "Adding welcome page: " + indexHtmlResource
					: "Adding welcome page template: index"));
			ParameterizableViewController controller = new ParameterizableViewController();
			controller.setViewName(welcomePage.getViewName());
			setRootHandler(controller);
		}
	}

	@Override
	public Object getHandlerInternal(HttpServletRequest request) throws Exception {
		return (!isHtmlTextAccepted(request)) ? null : super.getHandlerInternal(request);
	}

	private boolean isHtmlTextAccepted(HttpServletRequest request) {
		for (MediaType mediaType : getAcceptedMediaTypes(request)) {
			if (mediaType.includes(MediaType.TEXT_HTML)) {
				return true;
			}
		}
		return false;
	}

	private List<MediaType> getAcceptedMediaTypes(HttpServletRequest request) {
		String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
		if (StringUtils.hasText(acceptHeader)) {
			try {
				return MediaType.parseMediaTypes(acceptHeader);
			}
			catch (InvalidMediaTypeException ex) {
				logger.warn("Received invalid Accept header. Assuming all media types are accepted",
						logger.isDebugEnabled() ? ex : null);
			}
		}
		return MEDIA_TYPES_ALL;
	}

}
