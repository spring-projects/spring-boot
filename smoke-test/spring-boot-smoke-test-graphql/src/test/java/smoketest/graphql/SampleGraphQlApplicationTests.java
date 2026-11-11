/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.graphql;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * Tests for {@link SampleGraphQlApplication}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
class SampleGraphQlApplicationTests {

	@Autowired
	private HttpGraphQlTester graphQlTester;

	@Test
	void shouldFindSpringGraphQl() {
		this.graphQlTester.document("{ project(slug: \"spring-graphql\") { name } }")
			.execute()
			.path("project.name")
			.entity(String.class)
			.isEqualTo("Spring GraphQL");
	}

	@Test
	void shouldNotFindUnknownProject() {
		this.graphQlTester.document("{ project(slug: \"spring-unknown\") { name } }")
			.execute()
			.path("project.name")
			.pathDoesNotExist();
	}

}
