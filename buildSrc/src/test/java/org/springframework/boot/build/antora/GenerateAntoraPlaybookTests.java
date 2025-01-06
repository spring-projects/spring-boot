/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.build.antora;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.build.antora.Extensions.AntoraExtensionsConfiguration.ZipContentsCollector.AlwaysInclude;
import org.springframework.boot.build.antora.GenerateAntoraPlaybook.AntoraExtensions.ZipContentsCollector;
import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GenerateAntoraPlaybook}.
 *
 * @author Phillip Webb
 */
class GenerateAntoraPlaybookTests {

	@TempDir
	File temp;

	@Test
	void writePlaybookGeneratesExpectedContent() throws Exception {
		writePlaybookYml((task) -> {
			task.getAntoraExtensions().getXref().getStubs().addAll("appendix:.*", "api:.*", "reference:.*");
			ZipContentsCollector zipContentsCollector = task.getAntoraExtensions().getZipContentsCollector();
			zipContentsCollector.getAlwaysInclude().set(List.of(new AlwaysInclude("test", "local-aggregate-content")));
			zipContentsCollector.getDependencies().add("test-dependency");
		});
		String actual = Files.readString(this.temp.toPath()
			.resolve("rootproject/project/build/generated/docs/antora-playbook/antora-playbook.yml"));
		String expected = Files
			.readString(Path.of("src/test/resources/org/springframework/boot/build/antora/expected-playbook.yml"));
		assertThat(actual.replace('\\', '/')).isEqualToNormalizingNewlines(expected.replace('\\', '/'));
	}

	@Test
	void writePlaybookWhenHasJavadocExcludeGeneratesExpectedContent() throws Exception {
		writePlaybookYml((task) -> {
			task.getAntoraExtensions().getXref().getStubs().addAll("appendix:.*", "api:.*", "reference:.*");
			ZipContentsCollector zipContentsCollector = task.getAntoraExtensions().getZipContentsCollector();
			zipContentsCollector.getAlwaysInclude().set(List.of(new AlwaysInclude("test", "local-aggregate-content")));
			zipContentsCollector.getDependencies().add("test-dependency");
			task.getAsciidocExtensions().getExcludeJavadocExtension().set(true);
		});
		String actual = Files.readString(this.temp.toPath()
			.resolve("rootproject/project/build/generated/docs/antora-playbook/antora-playbook.yml"));
		assertThat(actual).doesNotContain("javadoc-extension");
	}

	private void writePlaybookYml(ThrowingConsumer<GenerateAntoraPlaybook> customizer) throws Exception {
		File rootProjectDir = new File(this.temp, "rootproject").getCanonicalFile();
		rootProjectDir.mkdirs();
		Project rootProject = ProjectBuilder.builder().withProjectDir(rootProjectDir).build();
		File projectDir = new File(rootProjectDir, "project");
		projectDir.mkdirs();
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).withParent(rootProject).build();
		project.getTasks()
			.register("generateAntoraPlaybook", GenerateAntoraPlaybook.class, customizer::accept)
			.get()
			.writePlaybookYml();
	}

}
