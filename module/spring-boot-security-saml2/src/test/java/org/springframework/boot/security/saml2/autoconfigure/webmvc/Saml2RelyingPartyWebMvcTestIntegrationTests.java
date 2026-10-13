/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.saml2.autoconfigure.webmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with SAML2.
 *
 * @author Dmytro Nosan
 */
@WebMvcTest(controllers = ExampleController.class, properties = {
		"spring.security.saml2.relyingparty.registration.test.entity-id=relyingparty",
		"spring.security.saml2.relyingparty.registration.test.assertingparty.entity-id=assertingparty",
		"spring.security.saml2.relyingparty.registration.test.assertingparty.singlesignon.url=https://example.com",
		"spring.security.saml2.relyingparty.registration.test.assertingparty.singlesignon.sign-request=false" })
class Saml2RelyingPartyWebMvcTestIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldRedirectToLogin() throws Exception {
		this.mockMvc.perform(get("/one"))
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/saml2/authenticate?registrationId=test"));
	}

}
