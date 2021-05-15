/*
 * Copyright 2012-2020 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		request.setAttribute(SKIP_ATTRIBUTE, Boolean.TRUE);
		return true;
	}

}
