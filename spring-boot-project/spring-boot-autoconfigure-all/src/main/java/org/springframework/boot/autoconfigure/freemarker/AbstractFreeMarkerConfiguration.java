/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.freemarker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;

/**
 * Base class for shared FreeMarker configuration.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
abstract class AbstractFreeMarkerConfiguration {

	private final FreeMarkerProperties properties;

	private final List<FreeMarkerVariablesCustomizer> variablesCustomizers;

	protected AbstractFreeMarkerConfiguration(FreeMarkerProperties properties,
			ObjectProvider<FreeMarkerVariablesCustomizer> variablesCustomizers) {
		this.properties = properties;
		this.variablesCustomizers = variablesCustomizers.orderedStream().toList();
	}

	protected final FreeMarkerProperties getProperties() {
		return this.properties;
	}

	protected void applyProperties(FreeMarkerConfigurationFactory factory) {
		factory.setTemplateLoaderPaths(this.properties.getTemplateLoaderPath());
		factory.setPreferFileSystemAccess(this.properties.isPreferFileSystemAccess());
		factory.setDefaultEncoding(this.properties.getCharsetName());
		factory.setFreemarkerSettings(createFreeMarkerSettings());
		factory.setFreemarkerVariables(createFreeMarkerVariables());
	}

	private Properties createFreeMarkerSettings() {
		Properties settings = new Properties();
		settings.put("recognize_standard_file_extensions", "true");
		settings.putAll(this.properties.getSettings());
		return settings;
	}

	private Map<String, Object> createFreeMarkerVariables() {
		Map<String, Object> variables = new HashMap<>();
		for (FreeMarkerVariablesCustomizer customizer : this.variablesCustomizers) {
			customizer.customizeFreeMarkerVariables(variables);
		}
		return variables;
	}

}
