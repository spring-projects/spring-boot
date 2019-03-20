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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification utility for use with maven-invoker-plugin verification scripts.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public final class Verify {

	public static final String SAMPLE_APP = "org.test.SampleApplication";

	private Verify() {
	}

	public static void verifyJar(File file) throws Exception {
		new JarArchiveVerification(file, SAMPLE_APP).verify();
	}

	public static void verifyJar(File file, String main, String... scriptContents)
			throws Exception {
		verifyJar(file, main, true, scriptContents);
	}

	public static void verifyJar(File file, String main, boolean executable,
			String... scriptContents) throws Exception {
		new JarArchiveVerification(file, main).verify(executable, scriptContents);
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

	public static Properties verifyBuildInfo(File file, String group, String artifact,
			String name, String version) throws IOException {
		FileSystemResource resource = new FileSystemResource(file);
		Properties properties = PropertiesLoaderUtils.loadProperties(resource);
		assertThat(properties.get("build.group")).isEqualTo(group);
		assertThat(properties.get("build.artifact")).isEqualTo(artifact);
		assertThat(properties.get("build.name")).isEqualTo(name);
		assertThat(properties.get("build.version")).isEqualTo(version);
		return properties;
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
			assertThat(hasNonUnpackEntry(entryName))
					.as("Entry starting with " + entryName + " was an UNPACK entry")
					.isTrue();
		}

		public void assertHasUnpackEntry(String entryName) {
			assertThat(hasUnpackEntry(entryName))
					.as("Entry starting with " + entryName + " was not an UNPACK entry")
					.isTrue();
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
			throw new IllegalStateException(
					"Unable to find entry starting with " + entryName);
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

	private abstract static class AbstractArchiveVerification {

		private final File file;

		AbstractArchiveVerification(File file) {
			this.file = file;
		}

		public void verify() throws Exception {
			verify(true);
		}

		public void verify(boolean executable, String... scriptContents)
				throws Exception {
			assertThat(this.file).exists().isFile();

			if (scriptContents.length > 0 && executable) {
				String contents = new String(FileCopyUtils.copyToByteArray(this.file));
				contents = contents.substring(0, contents
						.indexOf(new String(new byte[] { 0x50, 0x4b, 0x03, 0x04 })));
				for (String content : scriptContents) {
					assertThat(contents).contains(content);
				}
			}

			if (!executable) {
				String contents = new String(FileCopyUtils.copyToByteArray(this.file));
				assertThat(contents).as("Is executable")
						.startsWith(new String(new byte[] { 0x50, 0x4b, 0x03, 0x04 }));
			}

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
			verifier.assertHasEntryNameStartingWith("BOOT-INF/lib/spring-context");
			verifier.assertHasEntryNameStartingWith("BOOT-INF/lib/spring-core");
			verifier.assertHasEntryNameStartingWith("BOOT-INF/lib/javax.servlet-api-3");
			assertThat(verifier
					.hasEntry("org/springframework/boot/loader/JarLauncher.class"))
							.as("Unpacked launcher classes").isTrue();
			assertThat(verifier
					.hasEntry("BOOT-INF/classes/org/test/SampleApplication.class"))
							.as("Own classes").isTrue();
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
			assertThat(manifest.getMainAttributes().getValue("Main-Class"))
					.isEqualTo("org.springframework.boot.loader.JarLauncher");
			assertThat(manifest.getMainAttributes().getValue("Start-Class"))
					.isEqualTo(this.main);
			assertThat(manifest.getMainAttributes().getValue("Not-Used"))
					.isEqualTo("Foo");
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
			verifier.assertHasEntryNameStartingWith(
					"WEB-INF/lib-provided/javax.servlet-api-3");
			assertThat(verifier
					.hasEntry("org/" + "springframework/boot/loader/JarLauncher.class"))
							.as("Unpacked launcher classes").isTrue();
			assertThat(verifier
					.hasEntry("WEB-INF/classes/org/" + "test/SampleApplication.class"))
							.as("Own classes").isTrue();
			assertThat(verifier.hasEntry("index.html")).as("Web content").isTrue();
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
			assertThat(manifest.getMainAttributes().getValue("Main-Class"))
					.isEqualTo("org.springframework.boot.loader.WarLauncher");
			assertThat(manifest.getMainAttributes().getValue("Start-Class"))
					.isEqualTo("org.test.SampleApplication");
			assertThat(manifest.getMainAttributes().getValue("Not-Used"))
					.isEqualTo("Foo");
		}

	}

	private static class ZipArchiveVerification extends AbstractArchiveVerification {

		ZipArchiveVerification(File file) {
			super(file);
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
			assertThat(manifest.getMainAttributes().getValue("Main-Class"))
					.isEqualTo("org.springframework.boot.loader.PropertiesLauncher");
			assertThat(manifest.getMainAttributes().getValue("Start-Class"))
					.isEqualTo("org.test.SampleApplication");
			assertThat(manifest.getMainAttributes().getValue("Not-Used"))
					.isEqualTo("Foo");
		}

	}

	private static class ModuleArchiveVerification extends AbstractArchiveVerification {

		ModuleArchiveVerification(File file) {
			super(file);
		}

		@Override
		protected void verifyZipEntries(ArchiveVerifier verifier) throws Exception {
			super.verifyZipEntries(verifier);
			verifier.assertHasEntryNameStartingWith("lib/spring-context");
			verifier.assertHasEntryNameStartingWith("lib/spring-core");
			verifier.assertHasNoEntryNameStartingWith("lib/javax.servlet-api-3");
			assertThat(verifier
					.hasEntry("org/" + "springframework/boot/loader/JarLauncher.class"))
							.as("Unpacked launcher classes").isFalse();
			assertThat(verifier.hasEntry("org/" + "test/SampleModule.class"))
					.as("Own classes").isTrue();
		}

		@Override
		protected void verifyManifest(Manifest manifest) throws Exception {
		}

	}

}
