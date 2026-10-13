/*
 * Copyright 2012-present the original author or authors.
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
