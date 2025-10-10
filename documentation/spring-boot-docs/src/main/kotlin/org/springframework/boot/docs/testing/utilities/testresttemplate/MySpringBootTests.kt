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
import java.time.Duration

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class MySpringBootTests(@Autowired val template: TestRestTemplate) {

	@Test
	fun testRequest() {
		val headers = template.getForEntity("/example", String::class.java).headers
		assertThat(headers.location).hasHost("other.example.com")
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

