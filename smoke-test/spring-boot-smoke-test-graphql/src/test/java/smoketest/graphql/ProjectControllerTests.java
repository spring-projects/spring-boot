/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.graphql;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest(ProjectController.class)
class ProjectControllerTests {

	@Autowired
	private GraphQlTester graphQlTester;

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
