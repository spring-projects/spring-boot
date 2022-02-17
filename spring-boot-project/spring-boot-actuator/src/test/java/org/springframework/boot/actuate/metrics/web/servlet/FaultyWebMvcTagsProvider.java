/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.core.instrument.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link WebMvcTagsProvider} used for testing that can be configured to fail when getting
 * tags or long task tags.
 *
 * @author Andy Wilkinson
 */
class FaultyWebMvcTagsProvider extends DefaultWebMvcTagsProvider {

	private final AtomicBoolean fail = new AtomicBoolean();

	FaultyWebMvcTagsProvider() {
		super(true);
	}

	@Override
	public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler,
			Throwable exception) {
		if (this.fail.compareAndSet(true, false)) {
			throw new RuntimeException();
		}
		return super.getTags(request, response, handler, exception);
	}

	@Override
	public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
		if (this.fail.compareAndSet(true, false)) {
			throw new RuntimeException();
		}
		return super.getLongRequestTags(request, handler);
	}

	void failOnce() {
		this.fail.set(true);
	}

}
