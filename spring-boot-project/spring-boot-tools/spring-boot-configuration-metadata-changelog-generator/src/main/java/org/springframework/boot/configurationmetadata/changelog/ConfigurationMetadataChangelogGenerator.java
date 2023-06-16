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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Generates a configuration metadata changelog. Requires three arguments:
 *
 * <ol>
 * <li>The path of a directory containing jar files from which the old metadata will be
 * extracted
 * <li>The path of a directory containing jar files from which the new metadata will be
 * extracted
 * <li>The path of a file to which the changelog will be written
 * </ol>
 *
 * The name of each directory will be used to name the old and new metadata in the
 * generated changelog
 *
 * @author Andy Wilkinson
 */
final class ConfigurationMetadataChangelogGenerator {

	private ConfigurationMetadataChangelogGenerator() {

	}

	public static void main(String[] args) throws IOException {
		ConfigurationMetadataDiff diff = ConfigurationMetadataDiff.of(
				NamedConfigurationMetadataRepository.from(new File(args[0])),
				NamedConfigurationMetadataRepository.from(new File(args[1])));
		try (ConfigurationMetadataChangelogWriter writer = new ConfigurationMetadataChangelogWriter(
				new FileWriter(new File(args[2])))) {
			writer.write(diff);
		}
		System.out.println("\nConfiguration metadata changelog written to '" + args[2] + "'");
	}

}
