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
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ReactorNettyHttpClientMapper}.
 *
 * @author Phillip Webb
 */
class ReactorNettyHttpClientMapperTests {

	@Test
	void ofWithCollectionCreatesComposite() {
		ReactorNettyHttpClientMapper one = (httpClient) -> new TestHttpClient(httpClient, "1");
		ReactorNettyHttpClientMapper two = (httpClient) -> new TestHttpClient(httpClient, "2");
		ReactorNettyHttpClientMapper three = (httpClient) -> new TestHttpClient(httpClient, "3");
		ReactorNettyHttpClientMapper compose = ReactorNettyHttpClientMapper.of(List.of(one, two, three));
		TestHttpClient httpClient = (TestHttpClient) compose.configure(new TestHttpClient());
		assertThat(httpClient.getContent()).isEqualTo("123");
	}

	@Test
	void ofWhenCollectionIsNullThrowsException() {
		Collection<ReactorNettyHttpClientMapper> mappers = null;
		assertThatIllegalArgumentException().isThrownBy(() -> ReactorNettyHttpClientMapper.of(mappers))
			.withMessage("Mappers must not be null");
	}

	@Test
	void ofWithArrayCreatesComposite() {
		ReactorNettyHttpClientMapper one = (httpClient) -> new TestHttpClient(httpClient, "1");
		ReactorNettyHttpClientMapper two = (httpClient) -> new TestHttpClient(httpClient, "2");
		ReactorNettyHttpClientMapper three = (httpClient) -> new TestHttpClient(httpClient, "3");
		ReactorNettyHttpClientMapper compose = ReactorNettyHttpClientMapper.of(one, two, three);
		TestHttpClient httpClient = (TestHttpClient) compose.configure(new TestHttpClient());
		assertThat(httpClient.getContent()).isEqualTo("123");
	}

	@Test
	void ofWhenArrayIsNullThrowsException() {
		ReactorNettyHttpClientMapper[] mappers = null;
		assertThatIllegalArgumentException().isThrownBy(() -> ReactorNettyHttpClientMapper.of(mappers))
			.withMessage("Mappers must not be null");
	}

	private static class TestHttpClient extends HttpClient {

		private final String content;

		TestHttpClient() {
			this.content = "";
		}

		TestHttpClient(HttpClient httpClient, String content) {
			this.content = (httpClient instanceof TestHttpClient testHttpClient) ? testHttpClient.content + content
					: content;
		}

		@Override
		public HttpClientConfig configuration() {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		protected HttpClient duplicate() {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		String getContent() {
			return this.content;
		}

	}

}
