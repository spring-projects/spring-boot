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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import io.micrometer.core.instrument.Tag;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * {@link Tag Tags} provider for an exchange performed by a
 * {@link org.springframework.web.reactive.function.client.WebClient}.
 *
 * @author Brian Clozel
 * @since 2.1.0
 */
@FunctionalInterface
public interface WebClientExchangeTagsProvider {

	/**
	 * Provide tags to be associated with metrics for the client exchange.
	 * @param request the client request
	 * @param response the server response (may be {@code null})
	 * @param throwable the exception (may be {@code null})
	 * @return tags to associate with metrics for the request and response exchange
	 */
	Iterable<Tag> tags(ClientRequest request, ClientResponse response,
			Throwable throwable);

}
