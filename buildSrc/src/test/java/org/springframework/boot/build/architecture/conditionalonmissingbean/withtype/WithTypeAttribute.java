/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.conditionalonmissingbean.withtype;

import org.springframework.boot.build.architecture.annotations.TestConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

class WithTypeAttribute {

	@Bean
	@TestConditionalOnMissingBean(type = "String")
	String helloWorld() {
		return "Hello World";
	}

}
