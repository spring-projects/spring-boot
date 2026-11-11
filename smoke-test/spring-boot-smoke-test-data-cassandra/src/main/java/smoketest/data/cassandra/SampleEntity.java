/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.cassandra;

import org.jspecify.annotations.Nullable;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table
public class SampleEntity {

	@PrimaryKey
	private @Nullable String id;

	private @Nullable String description;

	public @Nullable String getId() {
		return this.id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	public @Nullable String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

}
