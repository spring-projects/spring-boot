/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.test.autoconfigure.webmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureMockMvc @AutoConfigureMockMvc} and the ordering of Spring
 * Security's filter
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
@WithMockUser(username = "user", password = "secret")
@Import(AfterSecurityFilter.class)
class AutoConfigureMockMvcSecurityFilterOrderingIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void afterSecurityFilterShouldFindAUserPrincipal() {
		assertThat(this.mvc.get().uri("/one")).hasStatusOk().hasBodyTextEqualTo("user");
	}

}
