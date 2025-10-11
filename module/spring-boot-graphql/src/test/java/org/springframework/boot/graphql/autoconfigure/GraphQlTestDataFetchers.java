/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.graphql.autoconfigure;

import java.util.Arrays;
import java.util.List;

import graphql.schema.DataFetcher;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test utility class holding {@link DataFetcher} implementations.
 *
 * @author Brian Clozel
 */
public final class GraphQlTestDataFetchers {

	private static final List<Book> books = Arrays.asList(
			new Book("book-1", "GraphQL for beginners", 100, "John GraphQL"),
			new Book("book-2", "Harry Potter and the Philosopher's Stone", 223, "Joanne Rowling"),
			new Book("book-3", "Moby Dick", 635, "Moby Dick"), new Book("book-3", "Moby Dick", 635, "Moby Dick"));

	private GraphQlTestDataFetchers() {

	}

	public static DataFetcher<Book> getBookByIdDataFetcher() {
		return (environment) -> {
			String id = environment.getArgument("id");
			assertThat(id).isNotNull();
			return getBookById(id);
		};
	}

	public static DataFetcher<Flux<Book>> getBooksOnSaleDataFetcher() {
		return (environment) -> {
			Integer minPages = environment.getArgument("minPages");
			assertThat(minPages).isNotNull();
			return getBooksOnSale(minPages);
		};
	}

	public static @Nullable Book getBookById(String id) {
		return books.stream().filter((book) -> book.getId().equals(id)).findFirst().orElse(null);
	}

	public static Flux<Book> getBooksOnSale(int minPages) {
		return Flux.fromIterable(books).filter((book) -> book.getPageCount() >= minPages);
	}

}
