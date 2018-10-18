/*
 * Copyright 2012-2018 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * Default implementation of {@link WebMvcTagsProvider}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class DefaultWebMvcTagsProvider implements WebMvcTagsProvider {

	@Override
	public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response,
			Object handler, Throwable exception) {
		return Tags.of(WebMvcTags.method(request), WebMvcTags.uri(request, response),
				WebMvcTags.exception(exception), WebMvcTags.status(response),
				WebMvcTags.outcome(response));
	}

	@Override
	public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
		return Tags.of(WebMvcTags.method(request), WebMvcTags.uri(request, null));
	}

}
