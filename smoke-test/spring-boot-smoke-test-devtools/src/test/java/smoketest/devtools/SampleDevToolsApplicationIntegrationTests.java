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

package smoketest.devtools;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SampleDevToolsApplication}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleDevToolsApplicationIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testStaticResource() {
		this.restTestClient.get()
			.uri("/css/application.css")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("color: green;"));
	}

	@Test
	void testPublicResource() {
		this.restTestClient.get()
			.uri("/public.txt")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("public file"));
	}

	@Test
	void testClassResource() {
		this.restTestClient.get().uri("/application.properties").exchange().expectStatus().isNotFound();
	}

}
