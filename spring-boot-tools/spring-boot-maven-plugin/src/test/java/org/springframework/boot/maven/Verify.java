/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verification utility for use with maven-invoker-plugin verification scripts.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class Verify {

	public static final String SAMPLE_APP = "org.test.SampleApplication";

	public static void verifyJar(File file) throws Exception {
		new JarArchiveVerification(file, SAMPLE_APP).verify();
	}

	public static void verifyJar(File file, String main) throws Exception {
		new JarArchiveVerification(file, main).verify();
	}

	public static void verifyWar(File file) throws Exception {
		new WarArchiveVerification(file).verify();
	}

	public static void verifyZip(File file) throws Exception {
		new ZipArchiveVerification(file).verify();
	}

	public static void verifyModule(File file) throws Exception {
		new ModuleArchiveVerification(file).verify();
	}

	public static class ArchiveVerifier {

		private final ZipFile zipFile;
		private final Map<String, ZipEntry> content;

		public ArchiveVerifier(ZipFile zipFile) {
			this.zipFile = zipFile;
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			this.content = new HashMap<String, ZipEntry>();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				this.content.put(zipEntry.getName(), zipEntry);
			}
		}

		public void assertHasEntryNameStartingWith(String entry) {
			for (String name : this.content.keySet()) {
				if (name.startsWith(entry)) {
					return;
				}
			}
			throw new IllegalStateException("Expected entry starting with " + entry);
		}

		public void assertHasNoEntryNameStartingWith(String entry) {
			for (String name : this.content.keySet()) {
				if (name.startsWith(entry)) {
					throw new IllegalStateException("Entry starting with " + entry
							+ " should not have been found");
				}
			}
		}

		public void assertHasNonUnpackEntry(String entryName) {
			assertTrue("Entry starting with " + entryName + " was an UNPACK entry",
					hasNonUnpackEntry(entryName));
		}

		public void assertHasUnpackEntry(String entryName) {
			assertTrue("Entry starting with " + entryName + " was not an UNPACK entry",
					hasUnpackEntry(entryName));
		}

		private boolean hasNonUnpackEntry(String entryName) {
			return !hasUnpackEntry(entryName);
		}

		private boolean hasUnpackEntry(String entryName) {
			String comment = getEntryStartingWith(entryName).getComment();
			return comment != null && comment.startsWith("UNPACK:");
		}

		private ZipEntry getEntryStartingWith(String entryName) {
			for (Map.Entry<String, ZipEntry> entry : this.content.entrySet()) {
				if (entry.getKey().startsWith(entryName)) {
					return entry.getValue();
				}
			}
			throw new IllegalStateException("Unable to find entry starting with "
					+ entryName);
		}

		public boolean hasEntry(String entry) {
			return this.content.containsKey(entry);
		}

		public ZipEntry getEntry(String entry) {
			return this.content.get(entry);
		}

		public InputStream getEntryContent(String entry) throws IOException {
			ZipEntry zipEntry = getEntry(entry);
			if (zipEntry == null) {
				throw new IllegalArgumentException("No entry with name [" + entry + "]");
			}
			return this.zipFile.getInputStream(zipEntry);
		}

	}

	private static abstract class AbstractArchiveVerification {

		private final File file;

		public AbstractArchiveVerification(File file) {
			this.file = file;
		}

		public void verify() throws Exception {
			assertTrue("Archive missing", this.file.exists());
			assertTrue("Archive not a file", this.file.isFile());

			ZipFile zipFile = new ZipFile(this.file);
			try {
				ArchiveVerifier verifier = new ArchiveVerifier(zipFile);
				verifyZipEntries(verifier);
			}
			finally {
				zipFile.close();
			}
		}

		protected void verifyZipEntries(ArchiveVerifier verifier) throws Exception {
			verifyManifest(verifier);
		}

		private void verifyManifest(ArchiveVerifier verifier) throws Exception {
			Manifest manifest = new Manifest(
					verifier.getEntryContent("META-INF/MANIFEST.MF"));
			verifyManifest(manifest);
		}

		protected abstract void verifyManifest(Manifest manifest) throws Exception;

	}

	public static class JarArchiveVerification extends AbstractArchiveVerification {

		private final String main;

		public JarArchiveVerification(File file, String main) {
			super(file);
			this.main = main;
		}

		@Override
		protected void verifyZipEntries(ArchiveVerifier verifier) throws Exception {
			super.verifyZipEntries(verifier);
			verifier.assertHasEntryNameStartingWith("lib/spring-context");
			verifier.assertHasEntryNameStartingWith("lib/spring-core");
			verifier.assertHasEntryNameStartingWith("lib/javax.servlet-api-3");
			assertTrue("Unpacked launcher classes", verifier.hasEntry("org/"
					+ "springframework/boot/loader/JarLauncher.class"));
			assertTrue("Own classes", verifier.hasEntry("org/"
					+ "test/SampleApplication.class"));
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
			assertEquals("org.springframework.boot.loader.JarLauncher", manifest
					.getMainAttributes().getValue("Main-Class"));
			assertEquals(this.main, manifest.getMainAttributes().getValue("Start-Class"));
			assertEquals("Foo", manifest.getMainAttributes().getValue("Not-Used"));
		}
	}

	public static class WarArchiveVerification extends AbstractArchiveVerification {

		public WarArchiveVerification(File file) {
			super(file);
		}

		@Override
		protected void verifyZipEntries(ArchiveVerifier verifier) throws Exception {
			super.verifyZipEntries(verifier);
			verifier.assertHasEntryNameStartingWith("WEB-INF/lib/spring-context");
			verifier.assertHasEntryNameStartingWith("WEB-INF/lib/spring-core");
			verifier.assertHasEntryNameStartingWith("WEB-INF/lib-provided/javax.servlet-api-3");
			assertTrue("Unpacked launcher classes", verifier.hasEntry("org/"
					+ "springframework/boot/loader/JarLauncher.class"));
			assertTrue("Own classes", verifier.hasEntry("WEB-INF/classes/org/"
					+ "test/SampleApplication.class"));
			assertTrue("Web content", verifier.hasEntry("index.html"));
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
			assertEquals("org.springframework.boot.loader.WarLauncher", manifest
					.getMainAttributes().getValue("Main-Class"));
			assertEquals("org.test.SampleApplication", manifest.getMainAttributes()
					.getValue("Start-Class"));
			assertEquals("Foo", manifest.getMainAttributes().getValue("Not-Used"));
		}
	}

	private static class ZipArchiveVerification extends AbstractArchiveVerification {

		public ZipArchiveVerification(File file) {
			super(file);
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
			assertEquals("org.springframework.boot.loader.PropertiesLauncher", manifest
					.getMainAttributes().getValue("Main-Class"));
			assertEquals("org.test.SampleApplication", manifest.getMainAttributes()
					.getValue("Start-Class"));
			assertEquals("Foo", manifest.getMainAttributes().getValue("Not-Used"));
		}
	}

	private static class ModuleArchiveVerification extends AbstractArchiveVerification {

		public ModuleArchiveVerification(File file) {
			super(file);
		}

		@Override
		protected void verifyZipEntries(ArchiveVerifier verifier) throws Exception {
			super.verifyZipEntries(verifier);
			verifier.assertHasEntryNameStartingWith("lib/spring-context");
			verifier.assertHasEntryNameStartingWith("lib/spring-core");
			verifier.assertHasNoEntryNameStartingWith("lib/javax.servlet-api-3");
			assertFalse("Unpacked launcher classes", verifier.hasEntry("org/"
					+ "springframework/boot/loader/JarLauncher.class"));
			assertTrue("Own classes", verifier.hasEntry("org/"
					+ "test/SampleModule.class"));
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
		}

	}

}
