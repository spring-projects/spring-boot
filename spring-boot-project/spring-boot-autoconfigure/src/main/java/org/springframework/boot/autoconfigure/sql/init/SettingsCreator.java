/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.sql.init;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * Helpers class for creating {@link DatabaseInitializationSettings} from
 * {@link SqlInitializationProperties}.
 *
 * @author Andy Wilkinson
 */
final class SettingsCreator {

	/**
     * Private constructor for the SettingsCreator class.
     */
    private SettingsCreator() {
	}

	/**
     * Creates a new instance of {@link DatabaseInitializationSettings} based on the provided {@link SqlInitializationProperties}.
     * 
     * @param properties the {@link SqlInitializationProperties} containing the initialization properties
     * @return the created {@link DatabaseInitializationSettings} instance
     */
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

	/**
     * Returns a list of script locations based on the provided parameters.
     * If the locations list is not null, it is returned as is.
     * Otherwise, a fallback list is created and returned.
     * The fallback list includes optional script locations based on the provided fallback and platform parameters.
     * 
     * @param locations the list of script locations to be returned
     * @param fallback the fallback script name
     * @param platform the platform for which the script locations are being generated
     * @return a list of script locations
     */
    private static List<String> scriptLocations(List<String> locations, String fallback, String platform) {
		if (locations != null) {
			return locations;
		}
		List<String> fallbackLocations = new ArrayList<>();
		fallbackLocations.add("optional:classpath*:" + fallback + "-" + platform + ".sql");
		fallbackLocations.add("optional:classpath*:" + fallback + ".sql");
		return fallbackLocations;
	}

}
