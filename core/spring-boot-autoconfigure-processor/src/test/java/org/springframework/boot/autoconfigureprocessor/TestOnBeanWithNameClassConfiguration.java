/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigureprocessor;

/**
 * Test configuration with an annotated class.
 *
 * @author Phillip Webb
 */
@TestConditionalOnBean(name = "test", type = "java.io.OutputStream")
public class TestOnBeanWithNameClassConfiguration {

}
