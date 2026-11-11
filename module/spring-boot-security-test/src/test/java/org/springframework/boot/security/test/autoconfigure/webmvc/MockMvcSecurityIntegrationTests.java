/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.test.autoconfigure.webmvc;

import java.util.Base64;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MockMvc security.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
@TestPropertySource(properties = { "debug=true" })
class MockMvcSecurityIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private PathPatternRequestMatcher.@Nullable Builder pathPatternRequestMatcherBuilder;

	@Test
	@WithMockUser(username = "test", password = "test", roles = "USER")
	void okResponseWithMockUser() {
		assertThat(this.mvc.get().uri("/")).hasStatusOk();
	}

	@Test
	void unauthorizedResponseWithNoUser() {
		assertThat(this.mvc.get().uri("/").accept(MediaType.APPLICATION_JSON)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void okResponseWithBasicAuthCredentialsForKnownUser() {
		assertThat(this.mvc.get()
			.uri("/")
			.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes())))
			.hasStatusOk();
	}

	@Test
	void pathPatternRequestMatcherBuilderIsAvailable() {
		assertThat(this.pathPatternRequestMatcherBuilder).isNotNull();
	}

}
