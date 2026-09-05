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

package smoketest.groovytemplates;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.http.clients.redirects=dont-follow")
@AutoConfigureRestTestClient
class SampleGroovyTemplateApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testHome() {
		this.restTestClient.get().uri("/").exchangeSuccessfully().expectBody(String.class).value((body) -> {
			assertThat(body).contains("<title>Messages");
			assertThat(body).doesNotContain("layout:fragment");
		});
	}

	@Test
	void testCreate() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.set("text", "FOO text");
		map.set("summary", "FOO");
		this.restTestClient.post()
			.uri("/")
			.body(map)
			.exchange()
			.expectHeader()
			.value("Location", (location) -> assertThat(location).contains("localhost:" + this.port));
	}

	@Test
	void testCss() {
		this.restTestClient.get()
			.uri("/css/bootstrap.min.css")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("body"));
	}

}
