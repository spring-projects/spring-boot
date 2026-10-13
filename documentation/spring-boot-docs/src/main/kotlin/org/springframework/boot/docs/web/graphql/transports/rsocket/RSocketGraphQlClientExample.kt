/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.graphql.transports.rsocket

import org.springframework.graphql.client.RSocketGraphQlClient
import org.springframework.stereotype.Component
import java.time.Duration

// tag::builder[]
@Component
class RSocketGraphQlClientExample(private val builder: RSocketGraphQlClient.Builder<*>) {
// end::builder[]

	val graphQlClient = builder.tcp("example.spring.io", 8181)
		.route("graphql")
		.build()

	fun rsocketOverTcp() {
		// tag::request[]
		val book = graphQlClient.document(
			"""
			{
				bookById(id: "book-1"){
					id
					name
					pageCount
					author
				}
			}				
			"""
		)
			.retrieve("bookById").toEntity(Book::class.java)
		// end::request[]
		book.block(Duration.ofSeconds(5))
	}

	internal class Book
}

