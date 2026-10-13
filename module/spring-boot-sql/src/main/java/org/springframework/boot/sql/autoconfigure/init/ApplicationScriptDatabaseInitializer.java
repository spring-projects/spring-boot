/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.autoconfigure.init;

import org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Marker interface for a script-based database initializer that initializes the
 * application's database.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 * @see AbstractScriptDatabaseInitializer
 */
@ImportRuntimeHints(SqlInitializationScriptsRuntimeHints.class)
public interface ApplicationScriptDatabaseInitializer {

	/**
	 * Adapts {@link SqlInitializationProperties} to
	 * {@link DatabaseInitializationSettings}.
	 * @param properties the properties to adapt
	 * @return the settings
	 */
	static DatabaseInitializationSettings getSettings(SqlInitializationProperties properties) {
		return SettingsCreator.createFrom(properties);
	}

}
