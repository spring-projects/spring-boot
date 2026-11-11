/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.springframework.boot.configurationsample.TestAccess;
import org.springframework.boot.configurationsample.TestEndpoint;
import org.springframework.boot.configurationsample.TestReadOperation;

/**
 * A simple endpoint with no default override, with the same id as {@link SimpleEndpoint},
 * but with no access by default.
 *
 * @author Moritz Halbritter
 */
@TestEndpoint(id = "simple", defaultAccess = TestAccess.NONE)
public class SimpleEndpoint3 {

	@TestReadOperation
	public String invoke() {
		return "test";
	}

}
