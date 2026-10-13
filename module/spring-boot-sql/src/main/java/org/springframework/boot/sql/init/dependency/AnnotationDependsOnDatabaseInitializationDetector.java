/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init.dependency;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * {@link DependsOnDatabaseInitializationDetector} that detects beans annotated with
 * {@link DependsOnDatabaseInitialization}.
 *
 * @author Andy Wilkinson
 */
class AnnotationDependsOnDatabaseInitializationDetector implements DependsOnDatabaseInitializationDetector {

	@Override
	public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
		Set<String> dependentBeans = new HashSet<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			if (beanFactory.findAnnotationOnBean(beanName, DependsOnDatabaseInitialization.class, false) != null) {
				dependentBeans.add(beanName);
			}
		}
		return dependentBeans;
	}

}
