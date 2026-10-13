/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.objects.requireNonNullWithString;

import java.util.Objects;

class RequireNonNullWithString {

	void exampleMethod() {
		Objects.requireNonNull(new Object(), "Object cannot be null");
	}

}
