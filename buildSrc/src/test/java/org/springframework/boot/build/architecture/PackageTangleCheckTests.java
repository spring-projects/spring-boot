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

package org.springframework.boot.build.architecture;

import java.io.File;
import java.io.IOException;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PackageTangleCheck}.
 *
 * @author Andy Wilkinson
 */
class PackageTangleCheckTests {

	@TempDir
	File temp;

	@Test
	void whenPackagesAreTangledTaskFailsAndWritesAReport() throws Exception {
		prepareTask("tangled", (packageTangleCheck) -> {
			assertThatExceptionOfType(GradleException.class).isThrownBy(packageTangleCheck::checkForPackageTangles);
			assertThat(
					new File(packageTangleCheck.getProject().getBuildDir(), "checkForPackageTangles/failure-report.txt")
						.length())
				.isGreaterThan(0);
		});
	}

	@Test
	void whenPackagesAreNotTangledTaskSucceedsAndWritesAnEmptyReport() throws Exception {
		prepareTask("untangled", (packageTangleCheck) -> {
			packageTangleCheck.checkForPackageTangles();
			assertThat(
					new File(packageTangleCheck.getProject().getBuildDir(), "checkForPackageTangles/failure-report.txt")
						.length())
				.isEqualTo(0);
		});
	}

	private void prepareTask(String classes, Callback<PackageTangleCheck> callback) throws Exception {
		File projectDir = new File(this.temp, "project");
		projectDir.mkdirs();
		copyClasses(classes, projectDir);
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
		PackageTangleCheck packageTangleCheck = project.getTasks()
			.create("checkForPackageTangles", PackageTangleCheck.class,
					(task) -> task.setClasses(project.files("classes")));
		callback.accept(packageTangleCheck);
	}

	private void copyClasses(String name, File projectDir) throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource root = resolver.getResource("classpath:org/springframework/boot/build/architecture/" + name);
		FileSystemUtils.copyRecursively(root.getFile(),
				new File(projectDir, "classes/org/springframework/boot/build/architecture/" + name));

	}

	private interface Callback<T> {

		void accept(T item) throws Exception;

	}

}
