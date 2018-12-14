/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration tests for {@link GrabCommand}
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 */
public class GrabCommandIntegrationTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Rule
	public CliTester cli = new CliTester("src/test/resources/grab-samples/");

	@Before
	@After
	public void deleteLocalRepository() {
		System.clearProperty("grape.root");
		System.clearProperty("groovy.grape.report.downloads");
	}

	@Test
	public void grab() throws Exception {

		System.setProperty("grape.root", this.temp.getRoot().getAbsolutePath());
		System.setProperty("groovy.grape.report.downloads", "true");

		// Use --autoconfigure=false to limit the amount of downloaded dependencies
		String output = this.cli.grab("grab.groovy", "--autoconfigure=false");
		assertThat(new File(this.temp.getRoot(), "repository/joda-time/joda-time"))
				.isDirectory();
		// Should be resolved from local repository cache
		assertThat(output.contains("Downloading: file:")).isTrue();
	}

	@Test
	public void duplicateDependencyManagementBomAnnotationsProducesAnError() {
		try {
			this.cli.grab("duplicateDependencyManagementBom.groovy");
			fail();
		}
		catch (Exception ex) {
			assertThat(ex.getMessage())
					.contains("Duplicate @DependencyManagementBom annotation");
		}
	}

	@Test
	public void customMetadata() throws Exception {
		System.setProperty("grape.root", this.temp.getRoot().getAbsolutePath());
		File repository = new File(this.temp.getRoot().getAbsolutePath(), "repository");
		FileSystemUtils.copyRecursively(
				new File("src/test/resources/grab-samples/repository"), repository);
		this.cli.grab("customDependencyManagement.groovy", "--autoconfigure=false");
		assertThat(new File(repository, "javax/ejb/ejb-api/3.0")).isDirectory();
	}

}
