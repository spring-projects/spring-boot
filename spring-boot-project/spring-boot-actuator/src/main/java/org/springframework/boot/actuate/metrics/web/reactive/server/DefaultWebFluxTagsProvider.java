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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.util.Collections;
import java.util.List;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import org.springframework.web.server.ServerWebExchange;

/**
 * Default implementation of {@link WebFluxTagsProvider}.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class DefaultWebFluxTagsProvider implements WebFluxTagsProvider {

	private final boolean ignoreTrailingSlash;

	private final List<WebFluxTagsContributor> contributors;

	public DefaultWebFluxTagsProvider() {
		this(false);
	}

	/**
	 * Creates a new {@link DefaultWebFluxTagsProvider} that will provide tags from the
	 * given {@code contributors} in addition to its own.
	 * @param contributors the contributors that will provide additional tags
	 * @since 2.3.0
	 */
	public DefaultWebFluxTagsProvider(List<WebFluxTagsContributor> contributors) {
		this(false, contributors);
	}

	public DefaultWebFluxTagsProvider(boolean ignoreTrailingSlash) {
		this(ignoreTrailingSlash, Collections.emptyList());
	}

	/**
	 * Creates a new {@link DefaultWebFluxTagsProvider} that will provide tags from the
	 * given {@code contributors} in addition to its own.
	 * @param ignoreTrailingSlash whether trailing slashes should be ignored when
	 * determining the {@code uri} tag.
	 * @param contributors the contributors that will provide additional tags
	 * @since 2.3.0
	 */
	public DefaultWebFluxTagsProvider(boolean ignoreTrailingSlash, List<WebFluxTagsContributor> contributors) {
		this.ignoreTrailingSlash = ignoreTrailingSlash;
		this.contributors = contributors;
	}

	@Override
	public Iterable<Tag> httpRequestTags(ServerWebExchange exchange, Throwable exception) {
		Tags tags = Tags.of(WebFluxTags.method(exchange), WebFluxTags.uri(exchange, this.ignoreTrailingSlash),
				WebFluxTags.exception(exception), WebFluxTags.status(exchange), WebFluxTags.outcome(exchange));
		for (WebFluxTagsContributor contributor : this.contributors) {
			tags = tags.and(contributor.httpRequestTags(exchange, exception));
		}
		return tags;
	}

}
