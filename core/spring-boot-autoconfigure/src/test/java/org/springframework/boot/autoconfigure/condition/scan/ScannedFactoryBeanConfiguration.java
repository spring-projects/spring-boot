/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition.scan;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for a factory bean produced by a bean method on a configuration class
 * found through component scanning.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
public class ScannedFactoryBeanConfiguration {

	@Bean
	public FactoryBean<ScanBean> exampleBeanFactoryBean() {
		return new ScanFactoryBean("foo");
	}

}
