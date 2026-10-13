/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} to validate
 * {@link org.springframework.boot.jackson2.JsonComponent} beans are discovered.
 *
 * @author Stephane Nicoll
 */
@WebMvcTest(controllers = ExampleController2.class,
		properties = "spring.http.converters.preferred-json-mapper=jackson2")
class WebMvcTestJsonComponentIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldFindJsonComponent() {
		assertThat(this.mvc.post().uri("/two2/1234abcd")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{ "identifier2": "1234abcd" }
				""");
	}

}
