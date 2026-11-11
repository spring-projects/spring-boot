/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.test.autoconfigure;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Example {@code @Controller} to be tested with {@link GraphQlTest @GraphQlTest}.
 *
 * @author Brian Clozel
 */
@Controller
public class BookController {

	@QueryMapping
	public Book bookById(@Argument String id) {
		return new Book("42", "Sample Book", 100, "Jane Spring");
	}

}
