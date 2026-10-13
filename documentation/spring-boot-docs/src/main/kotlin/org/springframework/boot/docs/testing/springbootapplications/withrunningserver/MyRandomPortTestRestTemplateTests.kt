/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.withrunningserver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class MyRandomPortTestRestTemplateTests {

	@Test
	fun exampleTest(@Autowired restTemplate: TestRestTemplate) {
		val body = restTemplate.getForObject("/", String::class.java)
		assertThat(body).isEqualTo("Hello World")
	}

}

