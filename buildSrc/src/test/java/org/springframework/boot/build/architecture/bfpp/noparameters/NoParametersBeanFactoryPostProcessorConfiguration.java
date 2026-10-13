/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.bfpp.noparameters;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;

class NoParametersBeanFactoryPostProcessorConfiguration {

	@Bean
	static BeanFactoryPostProcessor noParametersBeanFactoryPostProcessor() {
		return (beanFactory) -> {
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
