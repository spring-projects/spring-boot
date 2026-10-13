/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init.dependency;

import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Detects beans that depend on database initialization. Implementations should be
 * registered in {@code META-INF/spring.factories} under the key
 * {@code org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public interface DependsOnDatabaseInitializationDetector {

	/**
	 * Detect beans defined in the given {@code beanFactory} that depend on database
	 * initialization. If no beans are detected, an empty set is returned.
	 * @param beanFactory bean factory to examine
	 * @return names of any beans that depend upon database initialization
	 */
	Set<String> detect(ConfigurableListableBeanFactory beanFactory);

}
