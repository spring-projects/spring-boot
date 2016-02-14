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

package org.springframework.boot.cli.command.install;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.util.FileSystemUtils;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Installer}
 *
 * @author Andy Wilkinson
 */
public class InstallerTests {

	public DependencyResolver resolver = mock(DependencyResolver.class);

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Installer installer;

	@Before
	public void setUp() throws IOException {
		System.setProperty("spring.home", "target");
		FileSystemUtils.deleteRecursively(new File("target/lib"));

		this.installer = new Installer(this.resolver);
	}

	@After
	public void cleanUp() {
		System.clearProperty("spring.home");
	}

	@Test
	public void installNewDependency() throws Exception {
		File foo = createTemporaryFile("foo.jar");
		given(this.resolver.resolve(Arrays.asList("foo"))).willReturn(Arrays.asList(foo));
		this.installer.install(Arrays.asList("foo"));
		assertThat(getNamesOfFilesInLib(), containsInAnyOrder("foo.jar", ".installed"));
	}

	@Test
	public void installAndUninstall() throws Exception {
		File foo = createTemporaryFile("foo.jar");
		given(this.resolver.resolve(Arrays.asList("foo"))).willReturn(Arrays.asList(foo));
		this.installer.install(Arrays.asList("foo"));
		this.installer.uninstall(Arrays.asList("foo"));
		assertThat(getNamesOfFilesInLib(), contains(".installed"));
	}

	@Test
	public void installAndUninstallWithCommonDependencies() throws Exception {
		File alpha = createTemporaryFile("alpha.jar");
		File bravo = createTemporaryFile("bravo.jar");
		File charlie = createTemporaryFile("charlie.jar");
		given(this.resolver.resolve(Arrays.asList("bravo")))
				.willReturn(Arrays.asList(bravo, alpha));
		given(this.resolver.resolve(Arrays.asList("charlie")))
				.willReturn(Arrays.asList(charlie, alpha));

		this.installer.install(Arrays.asList("bravo"));
		assertThat(getNamesOfFilesInLib(),
				containsInAnyOrder("alpha.jar", "bravo.jar", ".installed"));

		this.installer.install(Arrays.asList("charlie"));
		assertThat(getNamesOfFilesInLib(), containsInAnyOrder("alpha.jar", "bravo.jar",
				"charlie.jar", ".installed"));

		this.installer.uninstall(Arrays.asList("bravo"));
		assertThat(getNamesOfFilesInLib(),
				containsInAnyOrder("alpha.jar", "charlie.jar", ".installed"));

		this.installer.uninstall(Arrays.asList("charlie"));
		assertThat(getNamesOfFilesInLib(), containsInAnyOrder(".installed"));
	}

	@Test
	public void uninstallAll() throws Exception {
		File alpha = createTemporaryFile("alpha.jar");
		File bravo = createTemporaryFile("bravo.jar");
		File charlie = createTemporaryFile("charlie.jar");

		given(this.resolver.resolve(Arrays.asList("bravo")))
				.willReturn(Arrays.asList(bravo, alpha));
		given(this.resolver.resolve(Arrays.asList("charlie")))
				.willReturn(Arrays.asList(charlie, alpha));

		this.installer.install(Arrays.asList("bravo"));
		this.installer.install(Arrays.asList("charlie"));
		assertThat(getNamesOfFilesInLib(), containsInAnyOrder("alpha.jar", "bravo.jar",
				"charlie.jar", ".installed"));

		this.installer.uninstallAll();
		assertThat(getNamesOfFilesInLib(), containsInAnyOrder(".installed"));
	}

	private Set<String> getNamesOfFilesInLib() {
		Set<String> names = new HashSet<String>();
		for (File file : new File("target/lib").listFiles()) {
			names.add(file.getName());
		}
		return names;
	}

	private File createTemporaryFile(String name) throws IOException {
		File temporaryFile = this.tempFolder.newFile(name);
		temporaryFile.deleteOnExit();
		return temporaryFile;
	}

}
