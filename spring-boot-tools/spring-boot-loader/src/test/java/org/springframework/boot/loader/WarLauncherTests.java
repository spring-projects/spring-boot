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

package org.springframework.boot.loader;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.junit.Test;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WarLauncher}.
 *
 * @author Andy Wilkinson
 */
public class WarLauncherTests extends AbstractExecutableArchiveLauncherTests {

	@Test
	public void explodedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath() throws Exception {
		File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF"));
		WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot, true));
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).hasSize(2);
		assertThat(getUrls(archives)).containsOnly(new File(explodedRoot, "WEB-INF/classes").toURI().toURL(),
				new URL("jar:" + new File(explodedRoot, "WEB-INF/lib/foo.jar").toURI().toURL() + "!/"));
	}

	@Test
	public void archivedWarHasOnlyWebInfClassesAndContentsOWebInfLibOnClasspath() throws Exception {
		File jarRoot = createJarArchive("archive.war", "WEB-INF");
		WarLauncher launcher = new WarLauncher(new JarFileArchive(jarRoot));
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).hasSize(2);
		assertThat(getUrls(archives)).containsOnly(new URL("jar:" + jarRoot.toURI().toURL() + "!/WEB-INF/classes!/"),
				new URL("jar:" + jarRoot.toURI().toURL() + "!/WEB-INF/lib/foo.jar!/"));
	}

}
