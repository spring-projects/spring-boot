/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.testing.withspringsecurity;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(UserController.class)
class MySecurityTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@WithMockUser(roles = "ADMIN")
	void requestProtectedUrlWithUser() {
		assertThat(this.mvc.get().uri("/")).doesNotHaveFailed();
	}

}
