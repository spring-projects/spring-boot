/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.graphql.transports.rsocket;

import java.time.Duration;

import reactor.core.publisher.Mono;

import org.springframework.graphql.client.RSocketGraphQlClient;
import org.springframework.stereotype.Component;

// tag::builder[]
@Component
public class RSocketGraphQlClientExample {

	private final RSocketGraphQlClient graphQlClient;

	public RSocketGraphQlClientExample(RSocketGraphQlClient.Builder<?> builder) {
		this.graphQlClient = builder.tcp("example.spring.io", 8181).route("graphql").build();
	}
	// end::builder[]

	public void rsocketOverTcp() {
		// tag::request[]
		Mono<Book> book = this.graphQlClient.document("{ bookById(id: \"book-1\"){ id name pageCount author } }")
			.retrieve("bookById")
			.toEntity(Book.class);
		// end::request[]
		book.block(Duration.ofSeconds(5));
	}

	static class Book {

	}

}
