/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.cli;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link GrabCommand}
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 */
@ExtendWith(OutputCaptureExtension.class)
class GrabCommandIntegrationTests {

	@RegisterExtension
	CliTester cli;

	GrabCommandIntegrationTests(CapturedOutput output) {
		this.cli = new CliTester("src/test/resources/grab-samples/", output);
	}

	@BeforeEach
	@AfterEach
	void deleteLocalRepository() {
		System.clearProperty("grape.root");
		System.clearProperty("groovy.grape.report.downloads");
	}

	@Test
	void grab() throws Exception {

		System.setProperty("grape.root", this.cli.getTemp().getAbsolutePath());
		System.setProperty("groovy.grape.report.downloads", "true");

		// Use --autoconfigure=false to limit the amount of downloaded dependencies
		String output = this.cli.grab("grab.groovy", "--autoconfigure=false");
		assertThat(new File(this.cli.getTemp(), "repository/joda-time/joda-time")).isDirectory();
		// Should be resolved from local repository cache
		assertThat(output.contains("Downloading: file:")).isTrue();
	}

	@Test
	void duplicateDependencyManagementBomAnnotationsProducesAnError() {
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> this.cli.grab("duplicateDependencyManagementBom.groovy"))
				.withMessageContaining("Duplicate @DependencyManagementBom annotation");
	}

	@Test
	void customMetadata() throws Exception {
		System.setProperty("grape.root", this.cli.getTemp().getAbsolutePath());
		File repository = new File(this.cli.getTemp().getAbsolutePath(), "repository");
		FileSystemUtils.copyRecursively(new File("src/test/resources/grab-samples/repository"), repository);
		this.cli.grab("customDependencyManagement.groovy", "--autoconfigure=false");
		assertThat(new File(repository, "javax/ejb/ejb-api/3.0")).isDirectory();
	}

}
