/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Example {@link TestConfiguration @TestConfiguration} for
 * {@link OverrideAutoConfiguration @OverrideAutoConfiguration} tests.
 *
 * @author Phillip Webb
 */
@TestConfiguration(proxyBeanMethods = false)
public class ExampleTestConfig {

}
