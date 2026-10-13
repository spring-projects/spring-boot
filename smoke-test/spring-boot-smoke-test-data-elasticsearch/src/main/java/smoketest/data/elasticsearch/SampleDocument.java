/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.elasticsearch;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * Sample document.
 *
 * @author Moritz Halbritter
 */
@Document(indexName = "examples")
public class SampleDocument {

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

	@Override
	public String toString() {
		return "SampleDocument{" + "id='" + this.id + '\'' + ", text='" + this.text + '\'' + '}';
	}

}
