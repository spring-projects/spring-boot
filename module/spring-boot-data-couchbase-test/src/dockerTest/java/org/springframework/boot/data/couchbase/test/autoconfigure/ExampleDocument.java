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

package org.springframework.boot.data.couchbase.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.id.GeneratedValue;
import org.springframework.data.couchbase.core.mapping.id.GenerationStrategy;

/**
 * Example document used with {@link DataCouchbaseTest @DataCouchbaseTest} tests.
 *
 * @author Eddú Meléndez
 */
@Document
public class ExampleDocument {

	@Id
	@GeneratedValue(strategy = GenerationStrategy.UNIQUE)
	@SuppressWarnings("NullAway.Init")
	private String id;

	private @Nullable String text;

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public @Nullable String getText() {
		return this.text;
	}

	public void setText(@Nullable String text) {
		this.text = text;
	}

}
