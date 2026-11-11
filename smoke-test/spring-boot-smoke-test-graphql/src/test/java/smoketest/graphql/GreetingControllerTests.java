/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.graphql;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureMockMvc
class GreetingControllerTests {

	@Autowired
	private HttpGraphQlTester graphQlTester;

	@Test
	void shouldUnauthorizeAnonymousUsers() {
		this.graphQlTester.documentName("greeting").variable("name", "Brian").execute().errors().satisfy((errors) -> {
			assertThat(errors).hasSize(1);
			assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
		});
	}

	@Test
	void shouldGreetWithSpecificName() {
		HttpGraphQlTester authenticated = withAdminCredentials(this.graphQlTester);
		authenticated.documentName("greeting")
			.variable("name", "Brian")
			.execute()
			.path("greeting")
			.entity(String.class)
			.isEqualTo("Hello, Brian!");
	}

	@Test
	void shouldGreetWithDefaultName() {
		HttpGraphQlTester authenticated = withAdminCredentials(this.graphQlTester);
		authenticated.document("{ greeting }")
			.execute()
			.path("greeting")
			.entity(String.class)
			.isEqualTo("Hello, Spring!");
	}

	private HttpGraphQlTester withAdminCredentials(HttpGraphQlTester graphQlTester) {
		return graphQlTester.mutate()
			.webTestClient(
					(httpClient) -> httpClient.defaultHeaders((headers) -> headers.setBasicAuth("admin", "admin")))
			.build();
	}

}
