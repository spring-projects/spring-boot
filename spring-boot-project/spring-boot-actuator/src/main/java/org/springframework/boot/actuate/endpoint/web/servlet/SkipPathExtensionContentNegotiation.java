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

package org.springframework.boot.actuate.endpoint.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@link HandlerInterceptor} to ensure that
 * {@link org.springframework.web.accept.PathExtensionContentNegotiationStrategy} is
 * skipped for web endpoints.
 *
 * @author Phillip Webb
 */
final class SkipPathExtensionContentNegotiation implements HandlerInterceptor {

	@SuppressWarnings("deprecation")
	private static final String SKIP_ATTRIBUTE = org.springframework.web.accept.PathExtensionContentNegotiationStrategy.class
		.getName() + ".SKIP";

	/**
     * This method is used to handle the pre-processing of a request before it is handled by the controller.
     * It sets an attribute in the request to skip the content negotiation for the path extension.
     * 
     * @param request  the HttpServletRequest object representing the incoming request
     * @param response  the HttpServletResponse object representing the outgoing response
     * @param handler  the Object representing the handler for the request
     * @return true if the request should be processed further, false otherwise
     * @throws Exception if an exception occurs during the processing of the request
     */
    @Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		request.setAttribute(SKIP_ATTRIBUTE, Boolean.TRUE);
		return true;
	}

}
