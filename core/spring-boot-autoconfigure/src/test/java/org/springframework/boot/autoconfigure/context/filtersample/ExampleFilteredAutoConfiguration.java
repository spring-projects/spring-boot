/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.context.filtersample;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public final class ExampleFilteredAutoConfiguration {

	@Bean
	String anotherExample() {
		return "fail";
	}

}
