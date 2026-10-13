/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.bfpp.parameters;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;

class ParametersBeanFactoryPostProcessorConfiguration {

	@Bean
	static BeanFactoryPostProcessor parametersBeanFactoryPostProcessor(Integer param) {
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
