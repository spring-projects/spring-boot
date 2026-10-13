/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import org.junit.platform.commons.annotation.Testable;

import org.springframework.context.annotation.Configuration;

@Testable
public abstract class AbstractJupiterTestWithConfigAndTestable {

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
