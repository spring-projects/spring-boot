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
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;

/**
 * Generates a configuration metadata changelog. Requires three arguments:
 *
 * <ol>
 * <li>The path of a directory containing jar files of the old version
 * <li>The path of a directory containing jar files of the new version
 * <li>The path of a file to which the asciidoc changelog will be written
 * </ol>
 *
 * The name of each directory will be used as version numbers in generated changelog.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.2.0
 */
public final class ChangelogGenerator {

	private ChangelogGenerator() {
	}

	public static void main(String[] args) throws IOException {
		generate(new File(args[0]), new File(args[1]), new File(args[2]));
	}

	private static void generate(File oldDir, File newDir, File out) throws IOException {
		String oldVersionNumber = oldDir.getName();
		ConfigurationMetadataRepository oldMetadata = buildRepository(oldDir);
		String newVersionNumber = newDir.getName();
		ConfigurationMetadataRepository newMetadata = buildRepository(newDir);
		Changelog changelog = Changelog.of(oldVersionNumber, oldMetadata, newVersionNumber, newMetadata);
		try (ChangelogWriter writer = new ChangelogWriter(out)) {
			writer.write(changelog);
		}
		System.out.println("%nConfiguration metadata changelog written to '%s'".formatted(out));
	}

	static ConfigurationMetadataRepository buildRepository(File directory) {
		ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
		for (File file : directory.listFiles()) {
			try (JarFile jarFile = new JarFile(file)) {
				JarEntry metadataEntry = jarFile.getJarEntry("META-INF/spring-configuration-metadata.json");
				if (metadataEntry != null) {
					builder.withJsonResource(jarFile.getInputStream(metadataEntry));
				}
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		return builder.build();
	}

}
