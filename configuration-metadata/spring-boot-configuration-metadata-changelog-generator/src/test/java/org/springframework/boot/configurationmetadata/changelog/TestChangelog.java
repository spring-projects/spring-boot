/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata.changelog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;

/**
 * Factory to create test {@link Changelog} instance.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
final class TestChangelog {

	private TestChangelog() {
	}

	static Changelog load() {
		ConfigurationMetadataRepository previousRepository = load("sample-1.0.json");
		ConfigurationMetadataRepository repository = load("sample-2.0.json");
		return Changelog.of("1.0", previousRepository, "2.0", repository);
	}

	private static ConfigurationMetadataRepository load(String filename) {
		try (InputStream inputStream = new FileInputStream("src/test/resources/" + filename)) {
			return ConfigurationMetadataRepositoryJsonBuilder.create(inputStream).build();
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
