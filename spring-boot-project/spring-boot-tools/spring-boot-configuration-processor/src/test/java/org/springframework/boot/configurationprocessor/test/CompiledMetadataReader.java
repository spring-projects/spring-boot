/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.configurationprocessor.test;

import java.io.InputStream;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;

/**
 * Read the contents of metadata generated from the {@link TestCompiler}.
 *
 * @author Scott Frederick
 */
public final class CompiledMetadataReader {

	private static final String METADATA_FILE = "META-INF/spring-configuration-metadata.json";

	private CompiledMetadataReader() {
	}

	public static ConfigurationMetadata getMetadata(Compiled compiled) {
		InputStream inputStream = compiled.getClassLoader().getResourceAsStream(METADATA_FILE);
		try {
			if (inputStream != null) {
				return new JsonMarshaller().read(inputStream);
			}
			else {
				return null;
			}
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to read metadata", ex);
		}
	}

}
