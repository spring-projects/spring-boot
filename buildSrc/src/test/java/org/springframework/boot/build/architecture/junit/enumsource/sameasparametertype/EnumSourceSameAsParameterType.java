/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.junit.enumsource.sameasparametertype;

import org.junit.jupiter.params.provider.EnumSource;

class EnumSourceSameAsParameterType {

	@EnumSource(Example.class)
	void exampleMethod(Example example) {

	}

	enum Example {

		ONE, TWO, THREE

	}

}
