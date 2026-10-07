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

package org.springframework.boot.build.antora;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import org.gradle.api.Project;
import org.gradle.api.tasks.VerificationException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link CheckJavadocMacros}.
 *
 * @author Moritz Halbritter
 */
class CheckJavadocMacrosTests {

	@TempDir
	private File temp;

	@Test
	void shouldAcceptClass() throws IOException {
		check("javadoc:java.lang.Object[]");
	}

	@Test
	void shouldAcceptMethodWithoutArguments() throws IOException {
		check("javadoc:java.lang.Object#hashCode()[]");
	}

	@Test
	void shouldAcceptMethodWithArguments() throws IOException {
		check("javadoc:java.lang.String#valueOf(int)[]");
	}

	@Test
	void shouldRejectMissingClass() {
		assertThatExceptionOfType(VerificationException.class).isThrownBy(() -> check("javadoc:java.lang.Missing[]"));
		assertThat(failureReport()).contains("class java.lang.Missing does not exist");
	}

	@Test
	void shouldRejectMissingMethodWithoutArguments() {
		assertThatExceptionOfType(VerificationException.class)
			.isThrownBy(() -> check("javadoc:java.lang.Object#missing()[]"));
		assertThat(failureReport()).contains("method missing() does not exist");
	}

	@Test
	void shouldRejectMethodWithWrongArguments() {
		assertThatExceptionOfType(VerificationException.class)
			.isThrownBy(() -> check("javadoc:java.lang.Object#hashCode(int)[]"));
		assertThat(failureReport()).contains("method hashCode(int) does not exist");
	}

	@Test
	void shouldRejectMethodWithWrongMultipleArguments() {
		assertThatExceptionOfType(VerificationException.class)
			.isThrownBy(() -> check("javadoc:java.lang.Object#hashCode(int, java.lang.String)[]"));
		assertThat(failureReport()).contains("method hashCode(int, java.lang.String) does not exist");
	}

	private void check(String content) throws IOException {
		File source = new File(this.temp, "source");
		source.mkdirs();
		Files.writeString(new File(source, "test.adoc").toPath(), content);
		Project project = ProjectBuilder.builder().withProjectDir(this.temp).build();
		CheckJavadocMacros task = project.getTasks().register("checkJavadocMacros", CheckJavadocMacros.class).get();
		task.setSource(project.files(source));
		task.setClasspath(project.files());
		task.getOutputDirectory().set(new File(this.temp, "output"));
		task.checkJavadocMacros();
	}

	private String failureReport() {
		try {
			return Files.readString(new File(this.temp, "output/failure-report.txt").toPath());
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
