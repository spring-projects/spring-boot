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

package org.springframework.boot.cli.command.install;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Installer}
 *
 * @author Andy Wilkinson
 */
class InstallerTests {

	private final DependencyResolver resolver = mock(DependencyResolver.class);

	@TempDir
	File tempDir;

	private Installer installer;

	@BeforeEach
	void setUp() throws IOException {
		System.setProperty("spring.home", this.tempDir.getAbsolutePath());
		this.installer = new Installer(this.resolver);
	}

	@AfterEach
	void cleanUp() {
		System.clearProperty("spring.home");
	}

	@Test
	void installNewDependency() throws Exception {
		File foo = createTemporaryFile("foo.jar");
		given(this.resolver.resolve(Arrays.asList("foo"))).willReturn(Arrays.asList(foo));
		this.installer.install(Arrays.asList("foo"));
		assertThat(getNamesOfFilesInLibExt()).containsOnly("foo.jar", ".installed");
	}

	@Test
	void installAndUninstall() throws Exception {
		File foo = createTemporaryFile("foo.jar");
		given(this.resolver.resolve(Arrays.asList("foo"))).willReturn(Arrays.asList(foo));
		this.installer.install(Arrays.asList("foo"));
		this.installer.uninstall(Arrays.asList("foo"));
		assertThat(getNamesOfFilesInLibExt()).contains(".installed");
	}

	@Test
	void installAndUninstallWithCommonDependencies() throws Exception {
		File alpha = createTemporaryFile("alpha.jar");
		File bravo = createTemporaryFile("bravo.jar");
		File charlie = createTemporaryFile("charlie.jar");
		given(this.resolver.resolve(Arrays.asList("bravo"))).willReturn(Arrays.asList(bravo, alpha));
		given(this.resolver.resolve(Arrays.asList("charlie"))).willReturn(Arrays.asList(charlie, alpha));
		this.installer.install(Arrays.asList("bravo"));
		assertThat(getNamesOfFilesInLibExt()).containsOnly("alpha.jar", "bravo.jar", ".installed");
		this.installer.install(Arrays.asList("charlie"));
		assertThat(getNamesOfFilesInLibExt()).containsOnly("alpha.jar", "bravo.jar", "charlie.jar", ".installed");
		this.installer.uninstall(Arrays.asList("bravo"));
		assertThat(getNamesOfFilesInLibExt()).containsOnly("alpha.jar", "charlie.jar", ".installed");
		this.installer.uninstall(Arrays.asList("charlie"));
		assertThat(getNamesOfFilesInLibExt()).containsOnly(".installed");
	}

	@Test
	void uninstallAll() throws Exception {
		File alpha = createTemporaryFile("alpha.jar");
		File bravo = createTemporaryFile("bravo.jar");
		File charlie = createTemporaryFile("charlie.jar");
		given(this.resolver.resolve(Arrays.asList("bravo"))).willReturn(Arrays.asList(bravo, alpha));
		given(this.resolver.resolve(Arrays.asList("charlie"))).willReturn(Arrays.asList(charlie, alpha));
		this.installer.install(Arrays.asList("bravo"));
		this.installer.install(Arrays.asList("charlie"));
		assertThat(getNamesOfFilesInLibExt()).containsOnly("alpha.jar", "bravo.jar", "charlie.jar", ".installed");
		this.installer.uninstallAll();
		assertThat(getNamesOfFilesInLibExt()).containsOnly(".installed");
	}

	private Set<String> getNamesOfFilesInLibExt() {
		Set<String> names = new HashSet<>();
		for (File file : new File(this.tempDir, "lib/ext").listFiles()) {
			names.add(file.getName());
		}
		return names;
	}

	private File createTemporaryFile(String name) throws IOException {
		File temporaryFile = new File(this.tempDir, name);
		temporaryFile.createNewFile();
		return temporaryFile;
	}

}
