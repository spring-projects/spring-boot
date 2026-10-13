/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} to add the {@link TestTypeExcludeFilter} to the
 * {@link ApplicationContext}.
 *
 * @author Phillip Webb
 */
class ExcludeFilterContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		TestTypeExcludeFilter.registerWith(context.getBeanFactory());
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
