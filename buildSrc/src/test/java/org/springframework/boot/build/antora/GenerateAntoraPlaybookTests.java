/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
			task.getXrefStubs().addAll("appendix:.*", "api:.*", "reference:.*");
			task.getAlwaysInclude().set(Map.of("name", "test", "classifier", "local-aggregate-content"));
		});
		String actual = Files.readString(this.temp.toPath()
			.resolve("rootproject/project/build/generated/docs/antora-playbook/antora-playbook.yml"));
		String expected = Files
			.readString(Path.of("src/test/resources/org/springframework/boot/build/antora/expected-playbook.yml"));
		System.out.println(actual);
		assertThat(actual.replace('\\', '/')).isEqualToNormalizingNewlines(expected.replace('\\', '/'));
	}

	private void writePlaybookYml(ThrowingConsumer<GenerateAntoraPlaybook> customizer) throws Exception {
		File rootProjectDir = new File(this.temp, "rootproject").getCanonicalFile();
		rootProjectDir.mkdirs();
		Project rootProject = ProjectBuilder.builder().withProjectDir(rootProjectDir).build();
		File projectDir = new File(rootProjectDir, "project");
		projectDir.mkdirs();
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).withParent(rootProject).build();
		GenerateAntoraPlaybook task = project.getTasks().create("generateAntoraPlaybook", GenerateAntoraPlaybook.class);
		customizer.accept(task);
		task.writePlaybookYml();
	}

}
