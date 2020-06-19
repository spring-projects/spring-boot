/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.data.r2dbc;

import javax.sql.DataSource;

import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.r2dbc.generate-unique-name=true")
class SampleR2dbcApplicationTests {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void citiesEndpointReturnInitialState() {
		this.webClient.get().uri("/cities").exchange().expectBody().jsonPath("$[*].id")
				.isEqualTo(new JSONArray().appendElement(2000).appendElement(2001));
	}

	@Test
	void citiesEndpointByIdWithExistingIdReturnCity() {
		this.webClient.get().uri("/cities/2001").exchange().expectBody().jsonPath("$.name").isEqualTo("San Francisco");
	}

	@Test
	void healthEndpointHasR2dbcEntry() {
		this.webClient.get().uri("/actuator/health").exchange().expectStatus().isOk().expectBody()
				.jsonPath("components.r2dbc.status").isEqualTo("UP").jsonPath("components.r2dbc.details.database")
				.isEqualTo("H2");
	}

	@Test
	void dataSourceIsNotAutoConfigured() {
		assertThat(this.applicationContext.getBeansOfType(DataSource.class)).isEmpty();
	}

}
