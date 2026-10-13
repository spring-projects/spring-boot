/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.objects.requireNonNullWithSupplier;

import java.util.Objects;

class RequireNonNullWithSupplier {

	void exampleMethod() {
		Objects.requireNonNull(new Object(), () -> "Object cannot be null");
	}

}
