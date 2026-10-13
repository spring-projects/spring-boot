/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init;

/**
 * Helper to get the settings used by the {@link AbstractScriptDatabaseInitializer}.
 *
 * @author Phillip Webb
 */
public final class ScriptDatabaseInitializerSettings {

	private ScriptDatabaseInitializerSettings() {
	}

	public static DatabaseInitializationSettings get(AbstractScriptDatabaseInitializer initializer) {
		return initializer.getSettings();
	}

}
