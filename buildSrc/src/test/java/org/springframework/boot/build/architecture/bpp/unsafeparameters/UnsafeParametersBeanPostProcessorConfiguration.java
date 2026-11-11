/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.bpp.unsafeparameters;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

class UnsafeParametersBeanPostProcessorConfiguration {

	@Bean
	static BeanPostProcessor unsafeParametersBeanPostProcessor(ApplicationContext context, Integer beanOne,
			String beanTwo) {
		return new BeanPostProcessor() {

		};
	}

	@Bean
	Integer beanOne() {
		return 1;
	}

	@Bean
	String beanTwo() {
		return "test";
	}

}
