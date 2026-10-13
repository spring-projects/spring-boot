/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init.dependency;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Base class for {@link DependsOnDatabaseInitializationDetector
 * DependsOnDatabaseInitializationDetectors} that detect by type beans that depend upon
 * database initialization.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public abstract class AbstractBeansOfTypeDependsOnDatabaseInitializationDetector
		implements DependsOnDatabaseInitializationDetector {

	@Override
	public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
		try {
			Set<Class<?>> types = getDependsOnDatabaseInitializationBeanTypes();
			return new BeansOfTypeDetector(types).detect(beanFactory);
		}
		catch (Throwable ex) {
			return Collections.emptySet();
		}
	}

	/**
	 * Returns the bean types that should be detected as depending on database
	 * initialization.
	 * @return the database initialization dependent bean types
	 */
	protected abstract Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes();

}
