/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 */
public class BootJarTests extends AbstractBootArchiveTests<BootJar> {

	public BootJarTests() {
		super(BootJar.class, "org.springframework.boot.loader.JarLauncher",
				"BOOT-INF/lib", "BOOT-INF/classes");
	}

	@Test
	public void contentCanBeAddedToBootInfUsingCopySpecFromGetter() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Application");
		bootJar.getBootInf().into("test")
				.from(new File("build.gradle").getAbsolutePath());
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchivePath())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	public void contentCanBeAddedToBootInfUsingCopySpecAction() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Application");
		bootJar.bootInf((copySpec) -> copySpec.into("test")
				.from(new File("build.gradle").getAbsolutePath()));
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchivePath())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Override
	protected void executeTask() {
		getTask().copy();
	}

}
