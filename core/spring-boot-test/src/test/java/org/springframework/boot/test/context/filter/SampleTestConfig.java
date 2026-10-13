/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@TestComponent
public class SampleTestConfig {

}
