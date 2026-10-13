/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} to validate converters are discovered.
 *
 * @author Stephane Nicoll
 */
@WebMvcTest(controllers = ExampleController2.class)
class WebMvcTestConverterIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldFindConverter() {
		String id = UUID.randomUUID().toString();
		assertThat(this.mvc.get().uri("/two/" + id)).hasStatusOk().hasBodyTextEqualTo(id + "two");
	}

}
