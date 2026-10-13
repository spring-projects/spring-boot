/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.utilities.testresttemplate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import java.time.Duration

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class MySpringBootTests(@Autowired val template: TestRestTemplate) {

	@Test
	fun testRequest() {
		val response = template.getForEntity("/example", String::class.java)
		assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
		// Other assertions to verify the response
	}

	@TestConfiguration(proxyBeanMethods = false)
	internal class RestTemplateBuilderConfiguration {

		@Bean
		fun restTemplateBuilder(): RestTemplateBuilder {
			return RestTemplateBuilder().connectTimeout(Duration.ofSeconds(1))
				.readTimeout(Duration.ofSeconds(1))
		}

	}

}

