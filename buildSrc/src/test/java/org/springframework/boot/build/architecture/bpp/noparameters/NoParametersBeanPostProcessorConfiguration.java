/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.bpp.noparameters;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;

class NoParametersBeanPostProcessorConfiguration {

	@Bean
	static BeanPostProcessor noParametersBeanPostProcessor() {
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
