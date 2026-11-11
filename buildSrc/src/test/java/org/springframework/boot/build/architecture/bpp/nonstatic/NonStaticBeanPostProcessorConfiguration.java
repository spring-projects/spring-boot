/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.bpp.nonstatic;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;

class NonStaticBeanPostProcessorConfiguration {

	@Bean
	BeanPostProcessor nonStaticBeanPostProcessor() {
		return new BeanPostProcessor() {

		};
	}

}
