/*
 * Copyright 2012-2014 the original author or authors.
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
import java.io.FileWriter;
import java.io.PrintWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.cli.command.grab.GrabCommand;
import org.springframework.util.FileSystemUtils;

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
	public void duplicateGrabMetadataAnnotationsProducesAnError() throws Exception {
		try {
			this.cli.grab("duplicateGrabMetadata.groovy");
			fail();
		}
		catch (Exception e) {
			assertTrue(e.getMessage().contains("Duplicate @GrabMetadata annotation"));
		}
	}

	@Test
	public void customMetadata() throws Exception {
		System.setProperty("grape.root", "target");

		File testArtifactDir = new File("target/repository/test/test/1.0.0");
		testArtifactDir.mkdirs();

		File testArtifact = new File(testArtifactDir, "test-1.0.0.properties");
		testArtifact.createNewFile();
		PrintWriter writer = new PrintWriter(new FileWriter(testArtifact));
		writer.println("javax.ejb\\:ejb-api=3.0");
		writer.close();

		this.cli.grab("customGrabMetadata.groovy", "--autoconfigure=false");
		assertTrue(new File("target/repository/javax/ejb/ejb-api/3.0").isDirectory());
	}
}
