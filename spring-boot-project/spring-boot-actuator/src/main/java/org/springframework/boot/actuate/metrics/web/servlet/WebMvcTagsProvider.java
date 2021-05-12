/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Tag;

/**
 * Provides {@link Tag Tags} for Spring MVC-based request handling.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public interface WebMvcTagsProvider {

	/**
	 * Provides tags to be associated with metrics for the given {@code request} and
	 * {@code response} exchange.
	 * @param request the request
	 * @param response the response
	 * @param handler the handler for the request or {@code null} if the handler is
	 * unknown
	 * @param exception the current exception, if any
	 * @return tags to associate with metrics for the request and response exchange
	 */
	Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler,
			Throwable exception);

	/**
	 * Provides tags to be used by {@link LongTaskTimer long task timers}.
	 * @param request the HTTP request
	 * @param handler the handler for the request or {@code null} if the handler is
	 * unknown
	 * @return tags to associate with metrics recorded for the request
	 */
	Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler);

}
