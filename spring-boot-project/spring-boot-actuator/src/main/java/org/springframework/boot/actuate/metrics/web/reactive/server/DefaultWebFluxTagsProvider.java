/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Arrays;

import io.micrometer.core.instrument.Tag;

import org.springframework.web.server.ServerWebExchange;

/**
 * Default implementation of {@link WebFluxTagsProvider}.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class DefaultWebFluxTagsProvider implements WebFluxTagsProvider {

	@Override
	public Iterable<Tag> httpRequestTags(ServerWebExchange exchange,
			Throwable exception) {
		return Arrays.asList(WebFluxTags.method(exchange), WebFluxTags.uri(exchange),
				WebFluxTags.exception(exception), WebFluxTags.status(exchange),
				WebFluxTags.outcome(exchange));
	}

}
