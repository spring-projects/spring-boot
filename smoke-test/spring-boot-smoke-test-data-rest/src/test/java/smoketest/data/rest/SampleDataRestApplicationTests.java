/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.rest;

import org.junit.jupiter.api.Test;
import smoketest.data.rest.domain.City;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
