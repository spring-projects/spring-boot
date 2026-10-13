/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.utilities.testresttemplate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.HttpStatus

class MyTests {

	private val template = TestRestTemplate()

	@Test
	fun testRequest() {
		val response = template.getForEntity("https://myhost.example.com/example", String::class.java)
		assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
		// Other assertions to verify the response
	}

}

