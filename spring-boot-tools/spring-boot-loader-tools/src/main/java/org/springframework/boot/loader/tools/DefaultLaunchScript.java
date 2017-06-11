/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.FileCopyUtils;

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

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final Pattern PLACEHOLDER_PATTERN = Pattern
			.compile("\\{\\{(\\w+)(:.*?)?\\}\\}(?!\\})");

	private static final List<String> FILE_PATH_KEYS = Arrays.asList("inlinedConfScript");

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

	/**
	 * Loads file contents.
	 * @param file File to load. If null, will load default launch.script
	 * @return String representation of file contents.
	 * @throws IOException if file is not found our can't be loaded.
	 */
	private String loadContent(File file) throws IOException {
		final byte[] fileBytes;
		if (file == null) {
			fileBytes = FileCopyUtils
					.copyToByteArray(getClass().getResourceAsStream("launch.script"));
		}
		else {
			fileBytes = FileCopyUtils.copyToByteArray(file);
		}
		return new String(fileBytes, UTF_8);
	}

	/**
	 * Replaces variable placeholders in file with specified property values.
	 * @param content String with variables defined in {{variable:default}} format.
	 * @param properties Key value pairs for variables to replace
	 * @return Updated String
	 * @throws IOException if a file property value or path is specified and the file
	 * cannot be loaded.
	 */
	private String expandPlaceholders(String content, Map<?, ?> properties)
			throws IOException {
		StringBuffer expanded = new StringBuffer();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
		while (matcher.find()) {
			String name = matcher.group(1);
			final String value;
			String defaultValue = matcher.group(2);
			if (properties != null && properties.containsKey(name)) {
				Object propertyValue = properties.get(name);
				if (FILE_PATH_KEYS.contains(name)) {
					value = parseFilePropertyValue(properties.get(name));
				}
				else {
					value = propertyValue.toString();
				}
			}
			else {
				value = (defaultValue == null ? matcher.group(0)
						: defaultValue.substring(1));
			}
			matcher.appendReplacement(expanded, value.replace("$", "\\$"));
		}
		matcher.appendTail(expanded);
		return expanded.toString();
	}

	/**
	 * Loads file based on File object or String path.
	 * @param propertyValue File Object or String path to file.
	 * @return File contents.
	 * @throws IOException if a file property value or path is specified and the file
	 * cannot be loaded.
	 */
	private String parseFilePropertyValue(Object propertyValue) throws IOException {
		if (propertyValue instanceof File) {
			return loadContent((File) propertyValue);
		}
		else {
			return loadContent(new File(propertyValue.toString()));
		}
	}

	/**
	 * The content of the launch script as a byte array.
	 * @return Byte representation of script.
	 */
	@Override
	public byte[] toByteArray() {
		return this.content.getBytes(UTF_8);
	}

}
