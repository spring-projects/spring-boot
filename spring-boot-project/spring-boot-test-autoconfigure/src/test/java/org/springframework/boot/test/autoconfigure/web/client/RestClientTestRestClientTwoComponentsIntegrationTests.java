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

package org.springframework.boot.test.autoconfigure.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest @RestClientTest} with two {@code RestClient} clients.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
@RestClientTest({ ExampleRestClientService.class, AnotherExampleRestClientService.class })
class RestClientTestRestClientTwoComponentsIntegrationTests {

	@Autowired
	private ExampleRestClientService client1;

	@Autowired
	private AnotherExampleRestClientService client2;

	@Autowired
	private MockServerRestClientCustomizer customizer;

	@Autowired
	private MockRestServiceServer server;

	@Test
	void serverShouldNotWork() {
		assertThatIllegalStateException().isThrownBy(
				() -> this.server.expect(requestTo(uri("/test"))).andRespond(withSuccess("hello", MediaType.TEXT_HTML)))
			.withMessageContaining("Unable to use auto-configured");
	}

	@Test
	void client1RestCallViaCustomizer() {
		this.customizer.getServer(this.client1.getRestClientBuilder())
			.expect(requestTo(uri("/test")))
			.andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(this.client1.test()).isEqualTo("hello");
	}

	@Test
	void client2RestCallViaCustomizer() {
		this.customizer.getServer(this.client2.getRestClientBuilder())
			.expect(requestTo(uri("/test")))
			.andRespond(withSuccess("there", MediaType.TEXT_HTML));
		assertThat(this.client2.test()).isEqualTo("there");
	}

	private static String uri(String path) {
		return "https://example.com" + path;
	}

}
