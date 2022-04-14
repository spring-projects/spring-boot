/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.web.graphql.transports.rsocket

import org.springframework.graphql.client.RSocketGraphQlClient
import java.net.URI
import java.time.Duration


class RSocketGraphQlClientExample {

	fun rsocketOverTcp() {
		// tag::tcp[]
		val client = RSocketGraphQlClient.builder()
			.tcp("example.spring.io", 8181)
			.route("graphql")
			.build()
		val book = client.document(
			"""
			{
				bookById(id: "book-1"){
					id
					name
					pageCount
					author
				}
			}				
			""")
			.retrieve("bookById").toEntity(Book::class.java)
		// end::tcp[]
		book.block(Duration.ofSeconds(5))
	}

	fun rsocketOverWebSocket() {
		// tag::websocket[]
		val client = RSocketGraphQlClient.builder()
			.webSocket(URI.create("wss://example.spring.io/rsocket"))
			.route("graphql")
			.build()
		val book = client.document(
			"""
			{
				bookById(id: "book-1"){
					id
					name
					pageCount
					author
				}
			}				
			""")
			.retrieve("bookById").toEntity(Book::class.java)
		// end::websocket[]
		book.block(Duration.ofSeconds(5))
	}

	internal class Book
}