/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init.dependency;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * Helper class for detecting beans of particular types in a bean factory.
 *
 * @author Andy Wilkinson
 */
class BeansOfTypeDetector {

	private final Set<Class<?>> types;

	BeansOfTypeDetector(Set<Class<?>> types) {
		this.types = types;
	}

	Set<String> detect(ListableBeanFactory beanFactory) {
		Set<String> beanNames = new HashSet<>();
		for (Class<?> type : this.types) {
			try {
				String[] names = beanFactory.getBeanNamesForType(type, true, false);
				Arrays.stream(names).map(BeanFactoryUtils::transformedBeanName).forEach(beanNames::add);
			}
			catch (Throwable ex) {
				// Continue
			}
		}
		return beanNames;
	}

}
