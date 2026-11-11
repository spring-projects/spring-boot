/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.domain.city;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.Field;

@Document
public class City {

	@Id
	private @Nullable String id;

	@Field
	private @Nullable String name;

}
