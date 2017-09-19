/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.web.servlet;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.Tag;

/**
 * Default implementation of {@link WebMvcTagsProvider}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class DefaultWebMvcTagsProvider implements WebMvcTagsProvider {

	/**
	 * Supplies default tags to long task timers.
	 * @param request The HTTP request.
	 * @param handler The request method that is responsible for handling the request.
	 * @return A set of tags added to every Spring MVC HTTP request
	 */
	@Override
	public Iterable<Tag> httpLongRequestTags(HttpServletRequest request, Object handler) {
		return Arrays.asList(WebMvcTags.method(request), WebMvcTags.uri(request));
	}

	/**
	 * Supplies default tags to the Web MVC server programming model.
	 * @param request The HTTP request.
	 * @param response The HTTP response.
	 * @param ex The current exception, if any
	 * @return A set of tags added to every Spring MVC HTTP request.
	 */
	@Override
	public Iterable<Tag> httpRequestTags(HttpServletRequest request,
			HttpServletResponse response, Throwable ex) {
		return Arrays.asList(WebMvcTags.method(request), WebMvcTags.uri(request),
				WebMvcTags.exception(ex), WebMvcTags.status(response));
	}

}
