/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init.dependency;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Base class for {@link DatabaseInitializerDetector DatabaseInitializerDetectors} that
 * detect database initializer beans by type.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public abstract class AbstractBeansOfTypeDatabaseInitializerDetector implements DatabaseInitializerDetector {

	@Override
	public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
		try {
			Set<Class<?>> types = getDatabaseInitializerBeanTypes();
			return new BeansOfTypeDetector(types).detect(beanFactory);
		}
		catch (Throwable ex) {
			return Collections.emptySet();
		}
	}

	/**
	 * Returns the bean types that should be detected as being database initializers.
	 * @return the database initializer bean types
	 */
	protected abstract Set<Class<?>> getDatabaseInitializerBeanTypes();

}
