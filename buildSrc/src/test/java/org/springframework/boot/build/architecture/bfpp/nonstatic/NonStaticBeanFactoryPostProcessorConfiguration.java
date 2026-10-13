/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.bfpp.nonstatic;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;

class NonStaticBeanFactoryPostProcessorConfiguration {

	@Bean
	BeanFactoryPostProcessor nonStaticBeanFactoryPostProcessor() {
		return (beanFactory) -> {
		};
	}

}
