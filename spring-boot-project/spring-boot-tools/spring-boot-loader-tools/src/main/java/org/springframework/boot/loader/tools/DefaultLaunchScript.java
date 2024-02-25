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

	private static final Set<String> FILE_PATH_KEYS = Collections.singleton("inlinedConfScript");

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
     * Loads the content of a file.
     * 
     * @param file the file to load the content from
     * @return the content of the file as a string
     * @throws IOException if an I/O error occurs while reading the file
     */
    private String loadContent(File file) throws IOException {
		if (file == null) {
			return loadContent(getClass().getResourceAsStream("launch.script"));
		}
		return loadContent(new FileInputStream(file));
	}

	/**
     * Loads the content from the given input stream and returns it as a string.
     * 
     * @param inputStream the input stream to load the content from
     * @return the content loaded from the input stream as a string
     * @throws IOException if an I/O error occurs while reading the input stream
     */
    private String loadContent(InputStream inputStream) throws IOException {
		try (inputStream) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			copy(inputStream, outputStream);
			return outputStream.toString(StandardCharsets.UTF_8);
		}
	}

	/**
     * Copies the contents of an InputStream to an OutputStream.
     * 
     * @param inputStream the InputStream to read from
     * @param outputStream the OutputStream to write to
     * @throws IOException if an I/O error occurs during the copying process
     */
    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}
		outputStream.flush();
	}

	/**
     * Expands placeholders in the given content using the provided properties.
     * 
     * @param content    the content with placeholders to be expanded
     * @param properties the map of properties to be used for expansion
     * @return the expanded content with placeholders replaced by their corresponding values
     * @throws IOException if an I/O error occurs while parsing file property values
     */
    private String expandPlaceholders(String content, Map<?, ?> properties) throws IOException {
		StringBuilder expanded = new StringBuilder();
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

	/**
     * Parses the property value and returns the content of the file.
     * 
     * @param propertyValue the value of the property to be parsed
     * @return the content of the file
     * @throws IOException if an I/O error occurs while loading the file content
     */
    private String parseFilePropertyValue(Object propertyValue) throws IOException {
		if (propertyValue instanceof File file) {
			return loadContent(file);
		}
		return loadContent(new File(propertyValue.toString()));
	}

	/**
     * Converts the content of the DefaultLaunchScript object to a byte array.
     * 
     * @return the content of the DefaultLaunchScript object as a byte array
     */
    @Override
	public byte[] toByteArray() {
		return this.content.getBytes(StandardCharsets.UTF_8);
	}

}
