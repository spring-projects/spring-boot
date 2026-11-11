/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.init;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;
import org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector;

/**
 * A {@link DatabaseInitializerDetector} for {@link R2dbcScriptDatabaseInitializer}.
 *
 * @author Andy Wilkinson
 */
class R2dbcScriptDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

	@Override
	protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
		return Collections.singleton(R2dbcScriptDatabaseInitializer.class);
	}

}
