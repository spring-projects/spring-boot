/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.configurationsample.TestAccess;
import org.springframework.boot.configurationsample.TestReadOperation;
import org.springframework.boot.configurationsample.TestWebEndpoint;

/**
 * A meta-annotated endpoint. Also with a package private read operation that has an
 * optional argument.
 *
 * @author Stephane Nicoll
 */
@TestWebEndpoint(id = "specific", defaultAccess = TestAccess.READ_ONLY)
public class SpecificEndpoint {

	@TestReadOperation
	String invoke(@Nullable String param) {
		return "test";
	}

}
