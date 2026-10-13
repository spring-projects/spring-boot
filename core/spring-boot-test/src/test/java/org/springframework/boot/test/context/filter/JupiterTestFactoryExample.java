/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class JupiterTestFactoryExample {

	@TestFactory
	Collection<DynamicNode> testFactory() {
		return Arrays.asList(DynamicTest.dynamicTest("Some dynamic test", () -> {
			// Test
		}));
	}

}
