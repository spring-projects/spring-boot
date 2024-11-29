/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.data.rest;

import org.junit.jupiter.api.Test;
import smoketest.data.rest.domain.City;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to run the application.
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureMockMvc
// Separate profile for web tests to avoid clashing databases
class SampleDataRestApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void testHome() {
		assertThat(this.mvc.get().uri("/api")).hasStatusOk().bodyText().contains("hotels");
	}

	@Test
	void findByNameAndCountry() {
		assertThat(this.mvc.get()
			.uri("/api/cities/search/findByNameAndCountryAllIgnoringCase?name=Melbourne&country=Australia"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$")
			.convertTo(City.class)
			.satisfies((city) -> {
				assertThat(city.getName()).isEqualTo("Melbourne");
				assertThat(city.getState()).isEqualTo("Victoria");
			});
	}

	@Test
	void findByContaining() {
		assertThat(this.mvc.get()
			.uri("/api/cities/search/findByNameContainingAndCountryContainingAllIgnoringCase?name=&country=UK"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("_embedded.cities")
			.asArray()
			.hasSize(3);
	}

}
