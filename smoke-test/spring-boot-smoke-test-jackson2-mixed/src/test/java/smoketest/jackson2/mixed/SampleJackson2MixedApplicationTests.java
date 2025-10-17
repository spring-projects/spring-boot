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

package smoketest.jackson2.mixed;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleJackson2MixedApplication}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleJackson2MixedApplicationTests {

	@Autowired
	RestTestClient rest;

	@Test
	void jackson2IsUsedForHttpMessageConversion() {
		this.rest.get().uri("/names").exchange().expectBody().json("[\"JACKSON2:Spring:Boot\"]");
	}

	@Test
	void jackson2IsUsedForActuatorEndpoints() {
		this.rest.get().uri("/actuator/names").exchange().expectBody().json("[\"JACKSON2:Spring:Boot\"]");
	}

	@Test
	@SuppressWarnings("unchecked")
	void configPropsShouldReturnOk() {
		this.rest.get()
			.uri("/actuator/configprops")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsOnlyKeys("contexts"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void actuatorLinksShouldReturnOk() {
		this.rest.get()
			.uri("/actuator")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsOnlyKeys("_links"));
	}

}
