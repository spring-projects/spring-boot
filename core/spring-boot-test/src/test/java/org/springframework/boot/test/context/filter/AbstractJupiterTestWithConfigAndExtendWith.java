/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public abstract class AbstractJupiterTestWithConfigAndExtendWith {

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
