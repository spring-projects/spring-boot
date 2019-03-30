/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.client;

import io.micrometer.core.instrument.Tag;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

/**
 * Provides {@link Tag Tags} for an exchange performed by a {@link RestTemplate}.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@FunctionalInterface
public interface RestTemplateExchangeTagsProvider {

	/**
	 * Provides the tags to be associated with metrics that are recorded for the given
	 * {@code request} and {@code response} exchange.
	 * @param urlTemplate the source URl template, if available
	 * @param request the request
	 * @param response the response (may be {@code null} if the exchange failed)
	 * @return the tags
	 */
	Iterable<Tag> getTags(String urlTemplate, HttpRequest request,
			ClientHttpResponse response);

}
