/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.junit.enumsource.inferredfromparametertype;

import org.junit.jupiter.params.provider.EnumSource;

class EnumSourceInferredFromParameterType {

	@EnumSource
	void exampleMethod(Example example) {

	}

	enum Example {

		ONE, TWO, THREE

	}

}
