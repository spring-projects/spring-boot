/*
 * Copyright 2012-present the original author or authors.
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

package smoketest.hateoas;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleHateoasApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void hasHalLinksWhenAnythingIsAcceptable() {
		this.restTestClient.get().uri("/customers/1").exchangeSuccessfully().expectBody(String.class).value((body) -> {
			assertThat(body).startsWith("{\"_links\":{\"self\":{\"href\"");
			assertThat(body).endsWith(",\"id\":1,\"firstName\":\"Oliver\",\"lastName\":\"Gierke\"}");
		});
	}

	@Test
	void hasHalLinksWhenJsonIsAcceptable() {
		this.restTestClient.get()
			.uri("/customers/1")
			.accept(MediaType.APPLICATION_JSON)
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> {
				assertThat(body).startsWith("{\"_links\":{\"self\":{\"href\"");
				assertThat(body).endsWith(",\"id\":1,\"firstName\":\"Oliver\",\"lastName\":\"Gierke\"}");
			});
	}

	@Test
	void producesJsonWhenXmlIsPreferred() {
		this.restTestClient.get()
			.uri("/customers/1")
			.header(HttpHeaders.ACCEPT, "application/xml;q=0.9,application/json;q=0.8")
			.exchangeSuccessfully()
			.expectHeader()
			.contentType(MediaType.APPLICATION_JSON);
	}

}
