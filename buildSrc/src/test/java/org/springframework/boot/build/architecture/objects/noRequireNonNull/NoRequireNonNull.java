/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.objects.noRequireNonNull;

import java.util.Collections;

import org.springframework.util.Assert;

class NoRequireNonNull {

	void exampleMethod() {
		Assert.notNull(new Object(), "Object must not be null");
		// Compilation of a method reference generates code that uses
		// Objects.requireNonNull(Object). Check that it doesn't cause a failure.
		Collections.emptyList().forEach(System.out::println);
	}

}
