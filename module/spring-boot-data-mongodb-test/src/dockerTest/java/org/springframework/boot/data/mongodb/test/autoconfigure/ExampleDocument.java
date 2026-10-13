/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Example document used with {@link DataMongoTest @DataMongoTest} tests.
 *
 * @author Michael Simons
 */
@Document(collection = "exampleDocuments")
public class ExampleDocument {

	private @Nullable String id;

	private @Nullable String text;

	public @Nullable String getId() {
		return this.id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	public @Nullable String getText() {
		return this.text;
	}

	public void setText(@Nullable String text) {
		this.text = text;
	}

}
