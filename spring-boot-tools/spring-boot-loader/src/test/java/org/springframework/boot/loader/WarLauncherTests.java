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

package org.springframework.boot.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.junit.Test;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WarLauncher}
 *
 * @author Andy Wilkinson
 */
public class WarLauncherTests {

	@Test
	public void explodedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath()
			throws Exception {
		File warRoot = new File("target/exploded-war");
		FileSystemUtils.deleteRecursively(warRoot);
		warRoot.mkdirs();
		File webInfClasses = new File(warRoot, "WEB-INF/classes");
		webInfClasses.mkdirs();
		File webInfLib = new File(warRoot, "WEB-INF/lib");
		webInfLib.mkdirs();
		File webInfLibFoo = new File(webInfLib, "foo.jar");
		new JarOutputStream(new FileOutputStream(webInfLibFoo)).close();
		WarLauncher launcher = new WarLauncher(new ExplodedArchive(warRoot, true));
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).hasSize(2);
		assertThat(getUrls(archives)).containsOnly(webInfClasses.toURI().toURL(),
				new URL("jar:" + webInfLibFoo.toURI().toURL() + "!/"));
	}

	@Test
	public void archivedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath()
			throws Exception {
		File warRoot = createWarArchive();
		WarLauncher launcher = new WarLauncher(new JarFileArchive(warRoot));
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).hasSize(2);
		assertThat(getUrls(archives)).containsOnly(
				new URL("jar:" + warRoot.toURI().toURL() + "!/WEB-INF/classes!/"),
				new URL("jar:" + warRoot.toURI().toURL() + "!/WEB-INF/lib/foo.jar!/"));
	}

	private Set<URL> getUrls(List<Archive> archives) throws MalformedURLException {
		Set<URL> urls = new HashSet<URL>(archives.size());
		for (Archive archive : archives) {
			urls.add(archive.getUrl());
		}
		return urls;
	}

	private File createWarArchive() throws IOException, FileNotFoundException {
		File warRoot = new File("target/archive.war");
		warRoot.delete();
		JarOutputStream jarOutputStream = new JarOutputStream(
				new FileOutputStream(warRoot));
		jarOutputStream.putNextEntry(new JarEntry("WEB-INF/"));
		jarOutputStream.putNextEntry(new JarEntry("WEB-INF/classes/"));
		jarOutputStream.putNextEntry(new JarEntry("WEB-INF/lib/"));
		JarEntry webInfLibFoo = new JarEntry("WEB-INF/lib/foo.jar");
		webInfLibFoo.setMethod(ZipEntry.STORED);
		ByteArrayOutputStream fooJarStream = new ByteArrayOutputStream();
		new JarOutputStream(fooJarStream).close();
		webInfLibFoo.setSize(fooJarStream.size());
		CRC32 crc32 = new CRC32();
		crc32.update(fooJarStream.toByteArray());
		webInfLibFoo.setCrc(crc32.getValue());
		jarOutputStream.putNextEntry(webInfLibFoo);
		jarOutputStream.write(fooJarStream.toByteArray());
		jarOutputStream.close();
		return warRoot;
	}

}
