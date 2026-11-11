/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * Example document used with {@link DataElasticsearchTest @DataElasticsearchTest} tests.
 *
 * @author Eddú Meléndez
 */
@Document(indexName = "examples")
public class ExampleDocument {

	@Id
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
