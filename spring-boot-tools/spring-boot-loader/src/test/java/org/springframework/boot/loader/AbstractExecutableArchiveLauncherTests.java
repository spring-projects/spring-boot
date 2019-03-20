/*
 * Copyright 2012-2016 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.util.FileCopyUtils;

/**
 * Base class for testing {@link ExecutableArchiveLauncher} implementations.
 *
 * @author Andy Wilkinson
 */
public class AbstractExecutableArchiveLauncherTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	protected File createJarArchive(String name, String entryPrefix) throws IOException {
		File archive = this.temp.newFile(name);
		JarOutputStream jarOutputStream = new JarOutputStream(
				new FileOutputStream(archive));
		jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/"));
		jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/classes/"));
		jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/lib/"));
		JarEntry libFoo = new JarEntry(entryPrefix + "/lib/foo.jar");
		libFoo.setMethod(ZipEntry.STORED);
		ByteArrayOutputStream fooJarStream = new ByteArrayOutputStream();
		new JarOutputStream(fooJarStream).close();
		libFoo.setSize(fooJarStream.size());
		CRC32 crc32 = new CRC32();
		crc32.update(fooJarStream.toByteArray());
		libFoo.setCrc(crc32.getValue());
		jarOutputStream.putNextEntry(libFoo);
		jarOutputStream.write(fooJarStream.toByteArray());
		jarOutputStream.close();
		return archive;
	}

	protected File explode(File archive) throws IOException {
		File exploded = this.temp.newFolder("exploded");
		JarFile jarFile = new JarFile(archive);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			File entryFile = new File(exploded, entry.getName());
			if (entry.isDirectory()) {
				entryFile.mkdirs();
			}
			else {
				FileCopyUtils.copy(jarFile.getInputStream(entry),
						new FileOutputStream(entryFile));
			}
		}
		jarFile.close();
		return exploded;
	}

	protected Set<URL> getUrls(List<Archive> archives) throws MalformedURLException {
		Set<URL> urls = new HashSet<URL>(archives.size());
		for (Archive archive : archives) {
			urls.add(archive.getUrl());
		}
		return urls;
	}

}
