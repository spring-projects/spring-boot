/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

class ResourceRegisteringAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		return new BeanFactoryInitializationAotContribution() {

			@Override
			public void applyTo(GenerationContext generationContext,
					BeanFactoryInitializationCode beanFactoryInitializationCode) {
				generationContext.getGeneratedFiles().addResourceFile("generated-resource", "content");
				generationContext.getGeneratedFiles().addResourceFile("nested/generated-resource", "nested content");
			}

		};
	}

}
