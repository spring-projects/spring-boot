/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.settings;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * DevTools settings loaded from {@literal /META-INF/spring-devtools.properties} files.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class DevToolsSettings {

	/**
	 * The location to look for settings properties. Can be present in multiple JAR files.
	 */
	public static final String SETTINGS_RESOURCE_LOCATION = "META-INF/spring-devtools.properties";

	private static DevToolsSettings settings;

	private final List<Pattern> restartIncludePatterns = new ArrayList<Pattern>();

	private final List<Pattern> restartExcludePatterns = new ArrayList<Pattern>();

	DevToolsSettings() {
	}

	void add(Map<?, ?> properties) {
		Map<String, Pattern> includes = getPatterns(properties, "restart.include.");
		this.restartIncludePatterns.addAll(includes.values());
		Map<String, Pattern> excludes = getPatterns(properties, "restart.exclude.");
		this.restartExcludePatterns.addAll(excludes.values());
	}

	private Map<String, Pattern> getPatterns(Map<?, ?> properties, String prefix) {
		Map<String, Pattern> patterns = new LinkedHashMap<String, Pattern>();
		for (Map.Entry<?, ?> entry : properties.entrySet()) {
			String name = String.valueOf(entry.getKey());
			if (name.startsWith(prefix)) {
				Pattern pattern = Pattern.compile((String) entry.getValue());
				patterns.put(name, pattern);
			}
		}
		return patterns;
	}

	public boolean isRestartInclude(URL url) {
		return isMatch(url.toString(), this.restartIncludePatterns);
	}

	public boolean isRestartExclude(URL url) {
		return isMatch(url.toString(), this.restartExcludePatterns);
	}

	private boolean isMatch(String url, List<Pattern> patterns) {
		for (Pattern pattern : patterns) {
			if (pattern.matcher(url).find()) {
				return true;
			}
		}
		return false;
	}

	public static DevToolsSettings get() {
		if (settings == null) {
			settings = load();
		}
		return settings;
	}

	static DevToolsSettings load() {
		return load(SETTINGS_RESOURCE_LOCATION);
	}

	static DevToolsSettings load(String location) {
		try {
			DevToolsSettings settings = new DevToolsSettings();
			Enumeration<URL> urls = Thread.currentThread().getContextClassLoader()
					.getResources(location);
			while (urls.hasMoreElements()) {
				settings.add(PropertiesLoaderUtils
						.loadProperties(new UrlResource(urls.nextElement())));
			}
			return settings;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to load devtools settings from "
					+ "location [" + location + "]", ex);
		}
	}

}
