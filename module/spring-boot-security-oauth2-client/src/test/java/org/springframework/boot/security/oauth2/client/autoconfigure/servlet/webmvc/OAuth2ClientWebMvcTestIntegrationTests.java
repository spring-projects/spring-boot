/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure.servlet.webmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with OAuth2.
 *
 * @author Dmytro Nosan
 */
@WebMvcTest(controllers = ExampleController.class,
		properties = { "spring.security.oauth2.client.registration.test.client-id=test",
				"spring.security.oauth2.client.registration.test.authorization-grant-type=authorization-code",
				"spring.security.oauth2.client.provider.test.authorization-uri=https://auth.example.org" })
class OAuth2ClientWebMvcTestIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldRedirectToLogin() throws Exception {
		this.mockMvc.perform(get("/one")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
	}

}
