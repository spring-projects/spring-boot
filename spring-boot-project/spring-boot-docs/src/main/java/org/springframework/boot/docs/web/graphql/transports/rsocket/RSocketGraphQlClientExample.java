/*
 * Copyright 2012-2023 the original author or authors.
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
