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

package org.springframework.boot.actuate.web.exchanges;

import java.util.List;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose {@link HttpExchange} information.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 3.0.0
 */
@Endpoint(id = "httpexchanges")
public class HttpExchangesEndpoint {

	private final HttpExchangeRepository repository;

	/**
	 * Create a new {@link HttpExchangesEndpoint} instance.
	 * @param repository the exchange repository
	 */
	public HttpExchangesEndpoint(HttpExchangeRepository repository) {
		Assert.notNull(repository, "Repository must not be null");
		this.repository = repository;
	}

	@ReadOperation
	public HttpExchangesDescriptor httpExchanges() {
		return new HttpExchangesDescriptor(this.repository.findAll());
	}

	/**
	 * Description of an application's {@link HttpExchange} entries.
	 */
	public static final class HttpExchangesDescriptor implements OperationResponseBody {

		private final List<HttpExchange> exchanges;

		private HttpExchangesDescriptor(List<HttpExchange> exchanges) {
			this.exchanges = exchanges;
		}

		public List<HttpExchange> getExchanges() {
			return this.exchanges;
		}

	}

}
