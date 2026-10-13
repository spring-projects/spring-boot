/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.conditionalonclass;

import org.springframework.boot.build.architecture.annotations.TestConditionalOnClass;
import org.springframework.context.annotation.Bean;

class OnBeanMethod {

	@Bean
	@TestConditionalOnClass(String.class)
	String helloWorld() {
		return "Hello World";
	}

}
