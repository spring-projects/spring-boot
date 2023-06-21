/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.util.Collection;

import reactor.netty.http.client.HttpClient;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;

/**
 * Mapper that allows for custom modification of a {@link HttpClient} before it is used as
 * the basis for a {@link ReactorClientHttpConnector}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface ReactorNettyHttpClientMapper {

	/**
	 * Configure the given {@link HttpClient} and return the newly created instance.
	 * @param httpClient the client to configure
	 * @return the new client instance
	 */
	HttpClient configure(HttpClient httpClient);

	/**
	 * Return a new {@link ReactorNettyHttpClientMapper} composed of the given mappers.
	 * @param mappers the mappers to compose
	 * @return a composed {@link ReactorNettyHttpClientMapper} instance
	 * @since 3.1.1
	 */
	static ReactorNettyHttpClientMapper of(Collection<ReactorNettyHttpClientMapper> mappers) {
		Assert.notNull(mappers, "Mappers must not be null");
		return of(mappers.toArray(ReactorNettyHttpClientMapper[]::new));
	}

	/**
	 * Return a new {@link ReactorNettyHttpClientMapper} composed of the given mappers.
	 * @param mappers the mappers to compose
	 * @return a composed {@link ReactorNettyHttpClientMapper} instance
	 * @since 3.1.1
	 */
	static ReactorNettyHttpClientMapper of(ReactorNettyHttpClientMapper... mappers) {
		Assert.notNull(mappers, "Mappers must not be null");
		return (httpClient) -> {
			for (ReactorNettyHttpClientMapper mapper : mappers) {
				httpClient = mapper.configure(httpClient);
			}
			return httpClient;
		};
	}

}
