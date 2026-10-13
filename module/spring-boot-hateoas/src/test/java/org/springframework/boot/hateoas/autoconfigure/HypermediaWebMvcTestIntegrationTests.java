/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hateoas.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebMvcTest @WebMvcTest} and Spring HATEOAS.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
class HypermediaWebMvcTestIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void plainResponse() {
		assertThat(this.mvc.get().uri("/hateoas/plain")).hasContentType("application/json");
	}

	@Test
	void hateoasResponse() {
		assertThat(this.mvc.get().uri("/hateoas/resource")).hasContentType("application/hal+json");
	}

	@SpringBootConfiguration
	@Import(HateoasController.class)
	static class TestConfiguration {

	}

	@RestController
	@RequestMapping("/hateoas")
	static class HateoasController {

		@RequestMapping("/resource")
		EntityModel<Map<String, String>> resource() {
			return EntityModel.of(new HashMap<>(), Link.of("self", LinkRelation.of("https://api.example.com")));
		}

		@RequestMapping("/plain")
		Map<String, String> plain() {
			return new HashMap<>();
		}

	}

}
