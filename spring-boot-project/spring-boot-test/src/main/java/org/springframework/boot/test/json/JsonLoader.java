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

package org.springframework.boot.test.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

/**
 * Internal helper used to load JSON from various sources.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class JsonLoader {

	private final Class<?> resourceLoadClass;

	private final Charset charset;

	/**
	 * Constructs a new JsonLoader object with the specified resource load class and
	 * charset.
	 * @param resourceLoadClass the class used for loading the JSON resource
	 * @param charset the character set used for decoding the JSON resource
	 */
	JsonLoader(Class<?> resourceLoadClass, Charset charset) {
		this.resourceLoadClass = resourceLoadClass;
		this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
	}

	/**
	 * Returns the resource load class.
	 * @return the resource load class
	 */
	Class<?> getResourceLoadClass() {
		return this.resourceLoadClass;
	}

	/**
	 * Retrieves the JSON content from the given source.
	 * @param source the source of the JSON content
	 * @return the JSON content as a string, or null if the source is null
	 */
	String getJson(CharSequence source) {
		if (source == null) {
			return null;
		}
		if (source.toString().endsWith(".json")) {
			return getJson(new ClassPathResource(source.toString(), this.resourceLoadClass));
		}
		return source.toString();
	}

	/**
	 * Retrieves the JSON content from the specified path using the provided resource load
	 * class.
	 * @param path The path to the JSON file.
	 * @param resourceLoadClass The class used for loading the resource.
	 * @return The JSON content as a string.
	 */
	String getJson(String path, Class<?> resourceLoadClass) {
		return getJson(new ClassPathResource(path, resourceLoadClass));
	}

	/**
	 * Converts a byte array to a JSON string.
	 * @param source the byte array to be converted
	 * @return the JSON string representation of the byte array
	 */
	String getJson(byte[] source) {
		return getJson(new ByteArrayInputStream(source));
	}

	/**
	 * Retrieves the JSON data from the specified file.
	 * @param source the file containing the JSON data
	 * @return the JSON data as a string
	 * @throws IllegalStateException if unable to load JSON from the file
	 */
	String getJson(File source) {
		try {
			return getJson(new FileInputStream(source));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load JSON from " + source, ex);
		}
	}

	/**
	 * Retrieves the JSON data from the given resource.
	 * @param source the resource containing the JSON data
	 * @return the JSON data as a string
	 * @throws IllegalStateException if unable to load JSON from the source
	 */
	String getJson(Resource source) {
		try {
			return getJson(source.getInputStream());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load JSON from " + source, ex);
		}
	}

	/**
	 * Reads the content of the given InputStream and returns it as a JSON string.
	 * @param source the InputStream containing the JSON content
	 * @return the JSON content as a string
	 * @throws IllegalStateException if unable to load JSON from the InputStream
	 */
	String getJson(InputStream source) {
		try {
			return FileCopyUtils.copyToString(new InputStreamReader(source, this.charset));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load JSON from InputStream", ex);
		}
	}

}
