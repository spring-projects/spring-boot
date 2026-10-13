/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition.scan;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for a factory bean produced by a bean method with arguments on a
 * configuration class found through component scanning.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
public class ScannedFactoryBeanWithBeanMethodArgumentsConfiguration {

	@Bean
	public Foo foo() {
		return new Foo();
	}

	@Bean
	public ScanFactoryBean exampleBeanFactoryBean(Foo foo) {
		return new ScanFactoryBean("foo");
	}

	static class Foo {

	}

}
