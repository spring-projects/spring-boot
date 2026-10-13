/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.autoconfigure.init;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * Helpers class for creating {@link DatabaseInitializationSettings} from
 * {@link SqlInitializationProperties}.
 *
 * @author Andy Wilkinson
 */
final class SettingsCreator {

	private SettingsCreator() {
	}

	static DatabaseInitializationSettings createFrom(SqlInitializationProperties properties) {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings
			.setSchemaLocations(scriptLocations(properties.getSchemaLocations(), "schema", properties.getPlatform()));
		settings.setDataLocations(scriptLocations(properties.getDataLocations(), "data", properties.getPlatform()));
		settings.setContinueOnError(properties.isContinueOnError());
		settings.setSeparator(properties.getSeparator());
		settings.setEncoding(properties.getEncoding());
		settings.setMode(properties.getMode());
		return settings;
	}

	private static List<String> scriptLocations(@Nullable List<String> locations, String fallback, String platform) {
		if (locations != null) {
			return locations;
		}
		List<String> fallbackLocations = new ArrayList<>();
		fallbackLocations.add("optional:classpath*:" + fallback + "-" + platform + ".sql");
		fallbackLocations.add("optional:classpath*:" + fallback + ".sql");
		return fallbackLocations;
	}

}
