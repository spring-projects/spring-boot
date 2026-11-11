/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.junit.enumsource.valuenecessary;

import org.junit.jupiter.params.provider.EnumSource;

class EnumSourceValueNecessary {

	@EnumSource(Example.class)
	void exampleMethod(String thing, Example example) {

	}

	enum Example {

		ONE, TWO, THREE

	}

}
