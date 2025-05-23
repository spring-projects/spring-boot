/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with SAML2.
 *
 * @author Dmytro Nosan
 */
@WebMvcTest(controllers = ExampleController1.class, properties = {
		"spring.security.saml2.relyingparty.registration.test.entity-id=relyingparty",
		"spring.security.saml2.relyingparty.registration.test.assertingparty.entity-id=assertingparty",
		"spring.security.saml2.relyingparty.registration.test.assertingparty.singlesignon.url=https://example.com",
		"spring.security.saml2.relyingparty.registration.test.assertingparty.singlesignon.sign-request=false" })
class WebMvcTestSaml2Tests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldRedirectToLogin() throws Exception {
		this.mockMvc.perform(get("/one"))
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/saml2/authenticate?registrationId=test"));
	}

}
