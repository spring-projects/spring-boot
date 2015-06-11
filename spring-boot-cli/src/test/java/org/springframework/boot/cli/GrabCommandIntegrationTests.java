/*
 * Copyright 2012-2015 the original author or authors.
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

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.util.FileSystemUtils;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration tests for {@link GrabCommand}
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 */
public class GrabCommandIntegrationTests {

	@Rule
	public CliTester cli = new CliTester("src/test/resources/grab-samples/");

	@Before
	public void setup() {
		Settings settings;
		try {
			settings = loadSettings();
		}
		catch (Exception e) {
			// be optimistic
			settings = null;
		}
		Assume.assumeTrue(
				"Grab command tests do not work with a local Maven repository declared",
				settings == null || settings.getLocalRepository() == null);

		deleteLocalRepository();
	}

	@After
	public void deleteLocalRepository() {
		FileSystemUtils.deleteRecursively(new File("target/repository"));
		System.clearProperty("grape.root");
		System.clearProperty("groovy.grape.report.downloads");
	}

	@Test
	public void grab() throws Exception {

		System.setProperty("grape.root", "target");
		System.setProperty("groovy.grape.report.downloads", "true");

		// Use --autoconfigure=false to limit the amount of downloaded dependencies
		String output = this.cli.grab("grab.groovy", "--autoconfigure=false");
		assertTrue(new File("target/repository/joda-time/joda-time").isDirectory());
		// Should be resolved from local repository cache
		assertTrue(output.contains("Downloading: file:"));
	}

	@Test
	public void duplicateDependencyManagementBomAnnotationsProducesAnError()
			throws Exception {
		try {
			this.cli.grab("duplicateDependencyManagementBom.groovy");
			fail();
		}
		catch (Exception ex) {
			assertThat(ex.getMessage(),
					containsString("Duplicate @DependencyManagementBom annotation"));
		}
	}

	@Test
	public void customMetadata() throws Exception {
		System.setProperty("grape.root", "target");
		FileSystemUtils.copyRecursively(new File(
				"src/test/resources/grab-samples/repository"), new File(
				"target/repository"));
		this.cli.grab("customDependencyManagement.groovy", "--autoconfigure=false");
		assertTrue(new File("target/repository/javax/ejb/ejb-api/3.0").isDirectory());
	}

	private Settings loadSettings() {
		File settingsFile = new File(System.getProperty("user.home"), ".m2/settings.xml");
		SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setUserSettingsFile(settingsFile);
		try {
			return new DefaultSettingsBuilderFactory().newInstance().build(request)
					.getEffectiveSettings();
		}
		catch (SettingsBuildingException ex) {
			throw new IllegalStateException("Failed to build settings from "
					+ settingsFile, ex);
		}
	}

}
