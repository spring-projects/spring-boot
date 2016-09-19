/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.gradle;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.assertj.core.api.Condition;
import org.assertj.core.description.TextDescription;
import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;

/**
 * Integration tests for Gradle repackaging with two different versions of the same
 * dependency.
 *
 * @author Andy Wilkinson
 */
public class MixedVersionRepackagingTests {

	private static final String BOOT_VERSION = Versions.getBootVersion();

	private static ProjectConnection project;

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("mixed-version-repackaging");
	}

	@Test
	public void singleVersionIsIncludedInJar() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=true",
						"-PexcludeDevtools=false")
				.run();
		File buildLibs = new File("target/mixed-version-repackaging/build/libs");
		File repackageFile = new File(buildLibs, "mixed-version-repackaging.jar");
		assertThat(repackageFile.exists()).isTrue();
		assertThat(new JarFile(repackageFile))
				.has(entryNamed("BOOT-INF/lib/guava-18.0.jar"));
		assertThat(new JarFile(repackageFile))
				.has(not(entryNamed("BOOT-INF/lib/guava-16.0.jar")));
	}

	private Condition<JarFile> entryNamed(String name) {
		return new JarFileEntryCondition(name);
	}

	private final class JarFileEntryCondition extends Condition<JarFile> {

		private final String entryName;

		private JarFileEntryCondition(String entryName) {
			super(new TextDescription("entry named '%s'", entryName));
			this.entryName = entryName;
		}

		@Override
		public boolean matches(JarFile jarFile) {
			return jarFile.getEntry(this.entryName) != null;
		}
	}

}
