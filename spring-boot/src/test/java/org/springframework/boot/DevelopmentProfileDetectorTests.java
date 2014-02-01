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

package org.springframework.boot;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link DevelopmentProfileDetector}.
 * 
 * @author Phillip Webb
 */
public class DevelopmentProfileDetectorTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private TestDevelopmentProfileDetector detector;

	private ConfigurableEnvironment environment;

	@Before
	public void setup() {
		this.detector = new TestDevelopmentProfileDetector();
		this.environment = mock(ConfigurableEnvironment.class);
	}

	@Test
	public void notFound() {
		this.detector.addDetectedProfiles(this.environment);
		verifyZeroInteractions(this.environment);
	}

	@Test
	public void foundDueToMavenBuild() throws Exception {
		this.temporaryFolder.newFile("pom.xml").createNewFile();
		this.detector.addDetectedProfiles(this.environment);
		verify(this.environment).addActiveProfile("development");
	}

	@Test
	public void foundDueToGradleBuild() throws Exception {
		this.temporaryFolder.newFile("build.gradle").createNewFile();
		this.detector.addDetectedProfiles(this.environment);
		verify(this.environment).addActiveProfile("development");
	}

	@Test
	public void foundDueToAntBuild() throws Exception {
		this.temporaryFolder.newFile("build.xml").createNewFile();
		this.detector.addDetectedProfiles(this.environment);
		verify(this.environment).addActiveProfile("development");
	}

	@Test
	public void differentProfileName() throws Exception {
		this.detector = new TestDevelopmentProfileDetector("different");
		this.temporaryFolder.newFile("pom.xml").createNewFile();
		this.detector.addDetectedProfiles(this.environment);
		verify(this.environment).addActiveProfile("different");
		verifyNoMoreInteractions(this.environment);
	}

	@Test
	public void notFoundWhenStartedFromJar() throws Exception {
		System.setProperty("sun.java.command", "something.jar");
		this.temporaryFolder.newFile("pom.xml").createNewFile();
		this.detector.setSkipPackageAsJar(false);
		this.detector.addDetectedProfiles(this.environment);
		verifyZeroInteractions(this.environment);
	}

	private class TestDevelopmentProfileDetector extends DevelopmentProfileDetector {

		private boolean skipPackageAsJar = true;

		public TestDevelopmentProfileDetector() {
			super();
		}

		public TestDevelopmentProfileDetector(String profileName) {
			super(profileName);
		}

		public void setSkipPackageAsJar(boolean skipPackageAsJar) {
			this.skipPackageAsJar = skipPackageAsJar;
		}

		@Override
		protected boolean isPackageAsJar() {
			if (this.skipPackageAsJar) {
				// Unfortunately surefire uses a jar so we need to stub this out
				return false;
			}
			return super.isPackageAsJar();
		}

		@Override
		protected File getUserDir() {
			return DevelopmentProfileDetectorTests.this.temporaryFolder.getRoot();
		}

	}
}
