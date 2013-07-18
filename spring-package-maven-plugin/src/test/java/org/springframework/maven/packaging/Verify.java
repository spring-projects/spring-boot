/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.maven.packaging;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verification utility for use with maven-invoker-plugin verification scripts.
 * 
 * @author Phillip Webb
 */
public class Verify {

	public static void verifyJar(File file) throws Exception {
		new JarArchiveVerification(file, "org.test.SampleApplication").verify();
	}

	public static void verifyJar(File file, String main) throws Exception {
		new JarArchiveVerification(file, main).verify();
	}

	public static void verifyWar(File file) throws Exception {
		new WarArchiveVerification(file).verify();
	}

	private static abstract class AbstractArchiveVerification {

		private File file;

		public AbstractArchiveVerification(File file) {
			this.file = file;
		}

		public void verify() throws Exception {
			assertTrue("Archive missing", this.file.exists());
			assertTrue("Archive not a file", this.file.isFile());

			ZipFile zipFile = new ZipFile(this.file);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			Map<String, ZipEntry> zipMap = new HashMap<String, ZipEntry>();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				zipMap.put(zipEntry.getName(), zipEntry);
			}
			verifyZipEntries(zipFile, zipMap);
			zipFile.close();
		}

		protected void verifyZipEntries(ZipFile zipFile, Map<String, ZipEntry> entries)
				throws Exception {
			verifyManifest(zipFile, entries.get("META-INF/MANIFEST.MF"));
		}

		private void verifyManifest(ZipFile zipFile, ZipEntry zipEntry) throws Exception {
			Manifest manifest = new Manifest(zipFile.getInputStream(zipEntry));
			verifyManifest(manifest);
		}

		protected abstract void verifyManifest(Manifest manifest) throws Exception;

		protected final void assertHasEntryNameStartingWith(
				Map<String, ZipEntry> entries, String value) {
			for (String name : entries.keySet()) {
				if (name.startsWith(value)) {
					return;
				}
			}
			throw new IllegalStateException("Expected entry starting with " + value);
		}
	}

	private static class JarArchiveVerification extends AbstractArchiveVerification {

		private String main;

		public JarArchiveVerification(File file, String main) {
			super(file);
			this.main = main;
		}

		@Override
		protected void verifyZipEntries(ZipFile zipFile, Map<String, ZipEntry> entries)
				throws Exception {
			super.verifyZipEntries(zipFile, entries);
			assertHasEntryNameStartingWith(entries, "lib/spring-context");
			assertHasEntryNameStartingWith(entries, "lib/spring-core");
			assertHasEntryNameStartingWith(entries, "lib/javax.servlet-api-3.0.1.jar");
			assertTrue("Unpacked launcher classes", entries.containsKey("org/"
					+ "springframework/launcher/JarLauncher.class"));
			assertTrue("Own classes", entries.containsKey("org/"
					+ "test/SampleApplication.class"));
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
			assertEquals("org.springframework.launcher.JarLauncher", manifest
					.getMainAttributes().getValue("Main-Class"));
			assertEquals(this.main, manifest.getMainAttributes().getValue("Start-Class"));
			assertEquals("Foo", manifest.getMainAttributes().getValue("Not-Used"));
		}
	}

	private static class WarArchiveVerification extends AbstractArchiveVerification {

		public WarArchiveVerification(File file) {
			super(file);
		}

		@Override
		protected void verifyZipEntries(ZipFile zipFile, Map<String, ZipEntry> entries)
				throws Exception {
			super.verifyZipEntries(zipFile, entries);
			assertHasEntryNameStartingWith(entries, "WEB-INF/lib/spring-context");
			assertHasEntryNameStartingWith(entries, "WEB-INF/lib/spring-core");
			assertHasEntryNameStartingWith(entries,
					"WEB-INF/lib-provided/javax.servlet-api-3.0.1.jar");
			assertTrue("Unpacked launcher classes", entries.containsKey("org/"
					+ "springframework/launcher/JarLauncher.class"));
			assertTrue("Own classes", entries.containsKey("WEB-INF/classes/org/"
					+ "test/SampleApplication.class"));
			assertTrue("Web content", entries.containsKey("index.html"));
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
			assertEquals("org.springframework.launcher.WarLauncher", manifest
					.getMainAttributes().getValue("Main-Class"));
			assertEquals("org.test.SampleApplication", manifest.getMainAttributes()
					.getValue("Start-Class"));
			assertEquals("Foo", manifest.getMainAttributes().getValue("Not-Used"));
		}
	}

}
