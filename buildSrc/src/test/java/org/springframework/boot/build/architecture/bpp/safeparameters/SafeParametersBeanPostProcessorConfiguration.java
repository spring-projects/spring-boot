/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.bpp.safeparameters;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

class SafeParametersBeanPostProcessorConfiguration {

	@Bean
	static BeanPostProcessor safeParametersBeanPostProcessor(ApplicationContext context, ObjectProvider<String> beanOne,
			ObjectProvider<String> beanTwo, Environment environment, @Lazy String beanThree) {
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
