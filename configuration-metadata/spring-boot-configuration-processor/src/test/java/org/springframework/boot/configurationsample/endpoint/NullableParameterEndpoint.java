/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.configurationsample.TestEndpoint;
import org.springframework.boot.configurationsample.TestReadOperation;

/**
 * An endpoint that uses {@code Nullable} to signal an optional parameter.
 *
 * @author Wonyong Hwang
 */
@TestEndpoint(id = "nullable")
public class NullableParameterEndpoint {

	@TestReadOperation
	public String invoke(@Nullable String parameter) {
		return "test with " + parameter;
	}

}
