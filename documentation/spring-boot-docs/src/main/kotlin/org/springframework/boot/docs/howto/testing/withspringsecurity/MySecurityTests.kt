/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.testing.withspringsecurity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.assertj.MockMvcTester

@WebMvcTest(UserController::class)
class MySecurityTests(@Autowired val mvc: MockMvcTester) {

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun requestProtectedUrlWithUser() {
		assertThat(mvc.get().uri("/"))
				.doesNotHaveFailed()
	}

}

