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

package org.springframework.boot.jdbc.test.autoconfigure;

import org.jspecify.annotations.Nullable;

/**
 * Example entity used with {@link JdbcTest @JdbcTest} tests.
 *
 * @author Stephane Nicoll
 */
public class ExampleEntity {

	private final int id;

	private @Nullable String name;

	public ExampleEntity(int id, @Nullable String name) {
		this.id = id;
		this.name = name;
	}

	public ExampleEntity(int id) {
		this(id, null);
	}

	public int getId() {
		return this.id;
	}

	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

}
