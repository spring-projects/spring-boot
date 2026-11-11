/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.springframework.boot.configurationsample.TestEndpoint;
import org.springframework.boot.configurationsample.TestReadOperation;

/**
 * An endpoint that is enabled unless configured explicitly.
 *
 * @author Stephane Nicoll
 */
@TestEndpoint(id = "enabled")
public class EnabledEndpoint {

	public String someMethod() {
		return "not a read operation";
	}

	@TestReadOperation
	public String retrieve(String parameter, Integer anotherParameter) {
		return "not a main read operation";
	}

}
