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

	/**
     * Private constructor for the ChangelogGenerator class.
     */
    private ChangelogGenerator() {
	}

	/**
     * Generates a Changelog file by reading the contents of a source file and writing it to a destination file.
     * 
     * @param sourceFile      the source file to read the contents from
     * @param destinationFile the destination file to write the contents to
     * @param changelogFile   the changelog file to generate
     * @throws IOException    if an I/O error occurs while reading or writing the files
     */
    public static void main(String[] args) throws IOException {
		generate(new File(args[0]), new File(args[1]), new File(args[2]));
	}

	/**
     * Generates a changelog for the configuration metadata between two versions.
     * 
     * @param oldDir The directory containing the old version of the configuration metadata.
     * @param newDir The directory containing the new version of the configuration metadata.
     * @param out The file to write the changelog to.
     * @throws IOException If an I/O error occurs while reading or writing the files.
     */
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

	/**
     * Builds a ConfigurationMetadataRepository by scanning the given directory for jar files
     * and extracting the spring-configuration-metadata.json file from each jar file.
     * 
     * @param directory the directory to scan for jar files
     * @return the built ConfigurationMetadataRepository
     * @throws RuntimeException if an IOException occurs while reading the jar files
     */
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
