/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DocumentAutoConfigurationClasses}.
 *
 * @author Andy Wilkinson
 */
class DocumentAutoConfigurationClassesTests {

	@TempDir
	private File temp;

	@Test
	void classesAreDocumented() throws IOException {
		File output = documentAutoConfigurationClasses((metadataDir) -> {
			writeAutoConfigurationMetadata("spring-boot-one", List.of("org.springframework.boot.one.AAutoConfiguration",
					"org.springframework.boot.one.BAutoConfiguration"), metadataDir);
			writeAutoConfigurationMetadata("spring-boot-two", List.of("org.springframework.boot.two.CAutoConfiguration",
					"org.springframework.boot.two.DAutoConfiguration"), metadataDir);
		});
		assertThat(output).isNotEmptyDirectory();
		assertThat(output.listFiles()).extracting(File::getName)
			.containsExactlyInAnyOrder("spring-boot-one.adoc", "spring-boot-two.adoc", "nav.adoc");
	}

	@Test
	void whenMetadataIsRemovedThenOutputForThatMetadataIsNoLongerPresent() throws IOException {
		documentAutoConfigurationClasses((metadataDir) -> {
			writeAutoConfigurationMetadata("spring-boot-one", List.of("org.springframework.boot.one.AAutoConfiguration",
					"org.springframework.boot.one.BAutoConfiguration"), metadataDir);
			writeAutoConfigurationMetadata("spring-boot-two", List.of("org.springframework.boot.two.CAutoConfiguration",
					"org.springframework.boot.two.DAutoConfiguration"), metadataDir);
		});
		File output = documentAutoConfigurationClasses(
				(metadataDir) -> assertThat(new File(metadataDir, "spring-boot-two.properties").delete()).isTrue());
		assertThat(output).isNotEmptyDirectory();
		assertThat(output.listFiles()).extracting(File::getName)
			.containsExactlyInAnyOrder("spring-boot-one.adoc", "nav.adoc");
	}

	private File documentAutoConfigurationClasses(Consumer<File> metadataDir) throws IOException {
		Project project = ProjectBuilder.builder().build();
		DocumentAutoConfigurationClasses task = project.getTasks()
			.register("documentAutoConfigurationClasses", DocumentAutoConfigurationClasses.class)
			.get();
		File output = new File(this.temp, "output");
		File input = new File(this.temp, "input");
		input.mkdirs();
		metadataDir.accept(input);
		ConfigurableFileCollection autoConfiguration = project.files();
		Stream.of(input.listFiles()).forEach(autoConfiguration::from);
		task.getOutputDir().set(output);
		task.setAutoConfiguration(autoConfiguration);
		task.documentAutoConfigurationClasses();
		return output;
	}

	private void writeAutoConfigurationMetadata(String module, List<String> classes, File outputDir) {
		File metadata = new File(outputDir, module + ".properties");
		Properties properties = new Properties();
		properties.setProperty("autoConfigurationClassNames", String.join(",", classes));
		properties.setProperty("module", module);
		try (FileOutputStream out = new FileOutputStream(metadata)) {
			properties.store(out, null);
		}
		catch (IOException ex) {
			throw new java.io.UncheckedIOException(ex);
		}
	}

}
