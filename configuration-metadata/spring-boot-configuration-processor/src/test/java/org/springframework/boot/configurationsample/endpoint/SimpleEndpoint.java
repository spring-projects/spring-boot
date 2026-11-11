/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.springframework.boot.configurationsample.TestEndpoint;
import org.springframework.boot.configurationsample.TestReadOperation;

/**
 * A simple endpoint with no default override.
 *
 * @author Stephane Nicoll
 */
@TestEndpoint(id = "simple")
public class SimpleEndpoint {

	@TestReadOperation
	public String invoke() {
		return "test";
	}

}
