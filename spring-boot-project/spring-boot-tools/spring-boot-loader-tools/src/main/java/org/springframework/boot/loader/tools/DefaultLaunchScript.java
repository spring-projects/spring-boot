/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link LaunchScript}. Provides the default Spring Boot launch
 * script or can load a specific script File. Also support mustache style template
 * expansion of the form <code>{{name:default}}</code>.
 *
 * @author Phillip Webb
 * @author Justin Rosenberg
 * @since 1.3.0
 */
public class DefaultLaunchScript implements LaunchScript {

	private static final int BUFFER_SIZE = 4096;

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)(:.*?)?\\}\\}(?!\\})");

	private static final Set<String> FILE_PATH_KEYS = Collections
			.unmodifiableSet(Collections.singleton("inlinedConfScript"));

	private final String content;

	/**
	 * Create a new {@link DefaultLaunchScript} instance.
	 * @param file the source script file or {@code null} to use the default
	 * @param properties an optional set of script properties used for variable expansion
	 * @throws IOException if the script cannot be loaded
	 */
	public DefaultLaunchScript(File file, Map<?, ?> properties) throws IOException {
		String content = loadContent(file);
		this.content = expandPlaceholders(content, properties);
	}

	private String loadContent(File file) throws IOException {
		if (file == null) {
			return loadContent(getClass().getResourceAsStream("launch.script"));
		}
		return loadContent(new FileInputStream(file));
	}

	private String loadContent(InputStream inputStream) throws IOException {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			copy(inputStream, outputStream);
			return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		}
		finally {
			inputStream.close();
		}
	}

	private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}
		outputStream.flush();
	}

	private String expandPlaceholders(String content, Map<?, ?> properties) throws IOException {
		StringBuffer expanded = new StringBuffer();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
		while (matcher.find()) {
			String name = matcher.group(1);
			final String value;
			String defaultValue = matcher.group(2);
			if (properties != null && properties.containsKey(name)) {
				Object propertyValue = properties.get(name);
				if (FILE_PATH_KEYS.contains(name)) {
					value = parseFilePropertyValue(propertyValue);
				}
				else {
					value = propertyValue.toString();
				}
			}
			else {
				value = (defaultValue != null) ? defaultValue.substring(1) : matcher.group(0);
			}
			matcher.appendReplacement(expanded, value.replace("$", "\\$"));
		}
		matcher.appendTail(expanded);
		return expanded.toString();
	}

	private String parseFilePropertyValue(Object propertyValue) throws IOException {
		if (propertyValue instanceof File) {
			return loadContent((File) propertyValue);
		}
		return loadContent(new File(propertyValue.toString()));
	}

	@Override
	public byte[] toByteArray() {
		return this.content.getBytes(StandardCharsets.UTF_8);
	}

}
