/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.r2dbc.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Example entity used with {@link DataR2dbcTest} tests.
 *
 * @author Mark Paluch
 */
@Table
public class Example {

	@Id
	@Nullable String id;

}
