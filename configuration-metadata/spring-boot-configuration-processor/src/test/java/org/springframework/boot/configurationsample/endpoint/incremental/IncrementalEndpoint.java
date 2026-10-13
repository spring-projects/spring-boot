/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint.incremental;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.configurationsample.TestEndpoint;
import org.springframework.boot.configurationsample.TestReadOperation;

/**
 * An endpoint that is enabled by default.
 *
 * @author Stephane Nicoll
 */
@TestEndpoint(id = "incremental")
public class IncrementalEndpoint {

	@TestReadOperation
	public String invoke(@Nullable String param) {
		return "test";
	}

}
