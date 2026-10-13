/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("abc")
class TestProfileConfiguration {

	@Bean
	public String abc() {
		return "abc";
	}

}
