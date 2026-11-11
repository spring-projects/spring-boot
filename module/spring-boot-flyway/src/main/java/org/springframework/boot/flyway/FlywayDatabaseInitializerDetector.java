/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway;

import java.util.Collections;
import java.util.Set;

import org.flywaydb.core.Flyway;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;
import org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector;

/**
 * A {@link DatabaseInitializerDetector} for {@link Flyway}.
 *
 * @author Andy Wilkinson
 */
class FlywayDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

	@Override
	protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
		return Collections.singleton(Flyway.class);
	}

}
