/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.conditionalonmissingbean.valueonly;

import org.springframework.boot.build.architecture.annotations.TestConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

class TypeSameAsMethodReturnType {

	@Bean
	@TestConditionalOnMissingBean(String.class)
	String helloWorld() {
		return "Hello World";
	}

}
