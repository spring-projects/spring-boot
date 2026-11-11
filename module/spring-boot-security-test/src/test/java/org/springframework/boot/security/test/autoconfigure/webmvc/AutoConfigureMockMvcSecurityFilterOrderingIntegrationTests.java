/*
 * Copyright 2012-present the original author or authors.
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
