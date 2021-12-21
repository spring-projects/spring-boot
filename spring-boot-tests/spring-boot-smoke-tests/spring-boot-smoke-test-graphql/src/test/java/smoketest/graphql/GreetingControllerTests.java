/*
 * Copyright 2012-2021 the original author or authors.
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

package smoketest.graphql;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureWebGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.WebGraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebGraphQlTester
class GreetingControllerTests {

	@Autowired
	private WebGraphQlTester graphQlTester;

	@Test
	void shouldUnauthorizeAnonymousUsers() {
		this.graphQlTester.queryName("greeting").variable("name", "Brian").execute().errors().satisfy((errors) -> {
			assertThat(errors).hasSize(1);
			assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
		});
	}

	@Test
	void shouldGreetWithSpecificName() {
		this.graphQlTester.queryName("greeting").variable("name", "Brian")
				.httpHeaders((headers) -> headers.setBasicAuth("admin", "admin")).execute().path("greeting")
				.entity(String.class).isEqualTo("Hello, Brian!");
	}

	@Test
	void shouldGreetWithDefaultName() {
		this.graphQlTester.query("{ greeting }").httpHeaders((headers) -> headers.setBasicAuth("admin", "admin"))
				.execute().path("greeting").entity(String.class).isEqualTo("Hello, Spring!");
	}

}
