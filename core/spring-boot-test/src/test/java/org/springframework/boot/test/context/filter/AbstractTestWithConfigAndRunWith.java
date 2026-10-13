/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Abstract test with nest {@code @Configuration} and {@code @RunWith} used by
 * {@link TestTypeExcludeFilter}.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
public abstract class AbstractTestWithConfigAndRunWith {

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
