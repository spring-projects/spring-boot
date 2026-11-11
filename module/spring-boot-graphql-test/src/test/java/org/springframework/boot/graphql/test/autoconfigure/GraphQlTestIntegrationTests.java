/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * Integration test for {@link GraphQlTest @GraphQlTest} annotated tests.
 *
 * @author Brian Clozel
 */
@GraphQlTest(BookController.class)
class GraphQlTestIntegrationTests {

	@Autowired
	private GraphQlTester graphQlTester;

	@Test
	void getBookdByIdShouldReturnTestBook() {
		String query = "{ bookById(id: \"book-1\"){ id name pageCount author } }";
		this.graphQlTester.document(query).execute().path("data.bookById.id").entity(String.class).isEqualTo("42");
	}

}
