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
