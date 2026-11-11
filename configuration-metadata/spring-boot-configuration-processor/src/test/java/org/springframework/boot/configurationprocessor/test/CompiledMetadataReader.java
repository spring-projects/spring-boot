/*
 * Copyright 2012-present the original author or authors.
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
 * @author Stephane Nicoll
 */
public final class CompiledMetadataReader {

	private static final String METADATA_FILE = "META-INF/spring-configuration-metadata.json";

	private CompiledMetadataReader() {
	}

	public static ConfigurationMetadata getMetadata(Compiled compiled) {
		return getMetadata(compiled, METADATA_FILE);
	}

	public static ConfigurationMetadata getMetadata(Compiled compiled, String location) {
		InputStream inputStream = compiled.getClassLoader().getResourceAsStream(location);
		try {
			if (inputStream != null) {
				return new JsonMarshaller().read(inputStream);
			}
			else {
				return null;
			}
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to read metadata from '%s'".formatted(location), ex);
		}
	}

}
