/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;
import org.springframework.boot.loader.zip.ZipContent;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NestedJarFile}.
 *
 * @author Phillip Webb
 * @author Martin Lau
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
@AssertFileChannelDataBlocksClosed
class NestedJarFileTests {

	@TempDir
	File tempDir;

	private File file;

	@BeforeEach
	void setup() throws Exception {
		this.file = new File(this.tempDir, "test.jar");
		TestJar.create(this.file);
	}

	@Test
	void createOpensJar() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			try (JarFile jdkJar = new JarFile(this.file)) {
				assertThat(jar.size()).isEqualTo(jdkJar.size());
				assertThat(jar.getComment()).isEqualTo(jdkJar.getComment());
				Enumeration<JarEntry> entries = jar.entries();
				Enumeration<JarEntry> jdkEntries = jdkJar.entries();
				while (entries.hasMoreElements()) {
					assertThat(entries.nextElement().getName()).isEqualTo(jdkEntries.nextElement().getName());
				}
				assertThat(jdkEntries.hasMoreElements()).isFalse();
				try (InputStream in = jar.getInputStream(jar.getEntry("1.dat"))) {
					assertThat(in.readAllBytes()).containsExactly(new byte[] { 1 });
				}
			}
		}
	}

	@Test
	void createWhenNestedJarFileOpensJar() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file, "nested.jar")) {
			assertThat(jar.size()).isEqualTo(5);
			assertThat(jar.stream().map(JarEntry::getName)).containsExactly("META-INF/", "META-INF/MANIFEST.MF",
					"3.dat", "4.dat", "\u00E4.dat");
		}
	}

	@Test
	void createWhenNestedJarDirectoryOpensJar() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file, "d/")) {
			assertThat(jar.getName()).isEqualTo(this.file.getAbsolutePath() + "!/d/");
			assertThat(jar.size()).isEqualTo(1);
			assertThat(jar.stream().map(JarEntry::getName)).containsExactly("9.dat");
		}
	}

	@Test
	void getManifestWhenNestedJarReturnsManifestOfNestedJar() throws Exception {
		try (JarFile jar = new JarFile(this.file)) {
			try (NestedJarFile nestedJar = new NestedJarFile(this.file, "nested.jar")) {
				Manifest manifest = nestedJar.getManifest();
				assertThat(manifest).isNotEqualTo(jar.getManifest());
				assertThat(manifest.getMainAttributes().getValue("Built-By")).isEqualTo("j2");
			}
		}
	}

	@Test
	void getManifestWhenNestedJarDirectoryReturnsManifestOfParent() throws Exception {
		try (JarFile jar = new JarFile(this.file)) {
			try (NestedJarFile nestedJar = new NestedJarFile(this.file, "d/")) {
				assertThat(nestedJar.getManifest()).isEqualTo(jar.getManifest());
			}
		}
	}

	@Test
	void createWhenJarHasFrontMatterOpensJar() throws IOException {
		File file = new File(this.tempDir, "frontmatter.jar");
		InputStream sourceJarContent = new FileInputStream(this.file);
		FileOutputStream outputStream = new FileOutputStream(file);
		StreamUtils.copy("#/bin/bash", Charset.defaultCharset(), outputStream);
		FileCopyUtils.copy(sourceJarContent, outputStream);
		try (NestedJarFile jar = new NestedJarFile(file)) {
			assertThat(jar.size()).isEqualTo(12);
		}
		try (NestedJarFile jar = new NestedJarFile(this.file, "nested.jar")) {
			assertThat(jar.size()).isEqualTo(5);
		}
	}

	@Test
	void getEntryReturnsEntry() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			JarEntry entry = jar.getEntry("1.dat");
			assertEntryOne(entry);
		}
	}

	@Test
	void getEntryWhenClosedThrowsException() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			jar.close();
			assertThatIllegalStateException().isThrownBy(() -> jar.getEntry("1.dat")).withMessage("Zip file closed");
		}
	}

	@Test
	void getJarEntryReturnsEntry() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			JarEntry entry = jar.getJarEntry("1.dat");
			assertEntryOne(entry);
		}
	}

	@Test
	void getJarEntryWhenClosedThrowsException() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			jar.close();
			assertThatIllegalStateException().isThrownBy(() -> jar.getJarEntry("1.dat")).withMessage("Zip file closed");
		}
	}

	private void assertEntryOne(JarEntry entry) {
		assertThat(entry.getName()).isEqualTo("1.dat");
		assertThat(entry.getRealName()).isEqualTo("1.dat");
		assertThat(entry.getSize()).isEqualTo(1);
		assertThat(entry.getCompressedSize()).isEqualTo(3);
		assertThat(entry.getCrc()).isEqualTo(2768625435L);
		assertThat(entry.getMethod()).isEqualTo(8);
	}

	@Test
	void getEntryWhenMultiReleaseEntryReturnsEntry() throws IOException {
		File multiReleaseFile = new File(this.tempDir, "mutli.zip");
		try (ZipContent zip = ZipContent.open(this.file.toPath(), "multi-release.jar")) {
			try (InputStream in = zip.openRawZipData().asInputStream()) {
				try (FileOutputStream out = new FileOutputStream(multiReleaseFile)) {
					in.transferTo(out);
				}
			}
		}
		try (NestedJarFile jar = new NestedJarFile(this.file, "multi-release.jar", JarFile.runtimeVersion())) {
			try (JarFile jdkJar = new JarFile(multiReleaseFile, true, ZipFile.OPEN_READ, JarFile.runtimeVersion())) {
				JarEntry entry = jar.getJarEntry("multi-release.dat");
				JarEntry jdkEntry = jdkJar.getJarEntry("multi-release.dat");
				assertThat(entry.getName()).isEqualTo(jdkEntry.getName());
				assertThat(entry.getRealName()).isEqualTo(jdkEntry.getRealName());
				try (InputStream inputStream = jdkJar.getInputStream(entry)) {
					assertThat(inputStream.available()).isOne();
					assertThat(inputStream.read()).isEqualTo(Runtime.version().feature());
				}
				try (InputStream inputStream = jar.getInputStream(entry)) {
					assertThat(inputStream.available()).isOne();
					assertThat(inputStream.read()).isEqualTo(Runtime.version().feature());
				}
			}
		}
	}

	@Test
	void getManifestReturnsManifest() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			Manifest manifest = jar.getManifest();
			assertThat(manifest).isNotNull();
			assertThat(manifest.getEntries()).isEmpty();
			assertThat(manifest.getMainAttributes().getValue("Manifest-Version")).isEqualTo("1.0");
		}
	}

	@Test
	void getCommentReturnsComment() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			assertThat(jar.getComment()).isEqualTo("outer");
		}
	}

	@Test
	void getCommentWhenClosedThrowsException() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			jar.close();
			assertThatIllegalStateException().isThrownBy(() -> jar.getComment()).withMessage("Zip file closed");
		}
	}

	@Test
	void getNameReturnsName() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			assertThat(jar.getName()).isEqualTo(this.file.getAbsolutePath());
		}
	}

	@Test
	void getNameWhenNestedReturnsName() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file, "nested.jar")) {
			assertThat(jar.getName()).isEqualTo(this.file.getAbsolutePath() + "!/nested.jar");
		}
	}

	@Test
	void sizeReturnsSize() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			assertThat(jar.size()).isEqualByComparingTo(12);
		}
	}

	@Test
	void sizeWhenClosedThrowsException() throws Exception {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			jar.close();
			assertThatIllegalStateException().isThrownBy(() -> jar.size()).withMessage("Zip file closed");
		}
	}

	@Test
	void getEntryTime() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			try (JarFile jdkJar = new JarFile(this.file)) {
				assertThat(jar.getEntry("META-INF/MANIFEST.MF").getTime())
					.isEqualTo(jar.getEntry("META-INF/MANIFEST.MF").getTime());
			}
		}
	}

	@Test
	void closeTriggersCleanupOnlyOnce() throws IOException {
		Cleaner cleaner = mock(Cleaner.class);
		ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
		Cleanable cleanable = mock(Cleanable.class);
		given(cleaner.register(any(), action.capture())).willReturn(cleanable);
		NestedJarFile jar = new NestedJarFile(this.file, null, null, false, cleaner);
		jar.close();
		jar.close();
		then(cleanable).should(atMostOnce()).clean();
		action.getValue().run();
	}

	@Test
	void cleanupFromReleasesResources() throws IOException {
		Cleaner cleaner = mock(Cleaner.class);
		ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
		Cleanable cleanable = mock(Cleanable.class);
		given(cleaner.register(any(), action.capture())).willReturn(cleanable);
		try (NestedJarFile jar = new NestedJarFile(this.file, null, null, false, cleaner)) {
			Object channel = Extractors.byName("resources.zipContent.data.fileAccess").apply(jar);
			assertThat(channel).extracting("referenceCount").isEqualTo(1);
			action.getValue().run();
			assertThat(channel).extracting("referenceCount").isEqualTo(0);
		}
	}

	@Test
	void getInputStreamReturnsInputStream() throws IOException {
		try (NestedJarFile jarFile = new NestedJarFile(this.file)) {
			JarEntry entry = jarFile.getJarEntry("2.dat");
			try (InputStream in = jarFile.getInputStream(entry)) {
				assertThat(in).hasBinaryContent(new byte[] { 0x02 });
			}
		}
	}

	@Test
	void getInputStreamWhenIsDirectory() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			try (InputStream inputStream = jar.getInputStream(jar.getEntry("d/"))) {
				assertThat(inputStream).isNotNull();
				assertThat(inputStream.read()).isEqualTo(-1);
			}
		}
	}

	@Test
	void getInputStreamWhenNameWithoutSlashAndIsDirectory() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file)) {
			try (InputStream inputStream = jar.getInputStream(jar.getEntry("d"))) {
				assertThat(inputStream).isNotNull();
				assertThat(inputStream.read()).isEqualTo(-1);
			}
		}
	}

	@Test
	void verifySignedJar() throws Exception {
		File signedJarFile = TestJar.getSigned();
		assertThat(signedJarFile).exists();
		try (JarFile expected = new JarFile(signedJarFile)) {
			try (NestedJarFile actual = new NestedJarFile(signedJarFile)) {
				StopWatch stopWatch = new StopWatch();
				Enumeration<JarEntry> actualEntries = actual.entries();
				while (actualEntries.hasMoreElements()) {
					JarEntry actualEntry = actualEntries.nextElement();
					JarEntry expectedEntry = expected.getJarEntry(actualEntry.getName());
					StreamUtils.drain(expected.getInputStream(expectedEntry));
					if (!actualEntry.getName().equals("META-INF/MANIFEST.MF")) {
						assertThat(actualEntry.getCertificates()).as(actualEntry.getName())
							.isEqualTo(expectedEntry.getCertificates());
						assertThat(actualEntry.getCodeSigners()).as(actualEntry.getName())
							.isEqualTo(expectedEntry.getCodeSigners());
					}
				}
				assertThat(stopWatch.getTotalTimeSeconds()).isLessThan(3.0);
			}
		}
	}

	@Test
	void closeAllowsFileToBeDeleted() throws Exception {
		new NestedJarFile(this.file).close();
		assertThat(this.file.delete()).isTrue();
	}

	@Test
	void streamStreamsEntries() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file, "multi-release.jar")) {
			assertThat(jar.stream().map((entry) -> entry.getName() + ":" + entry.getRealName())).containsExactly(
					"META-INF/:META-INF/", "META-INF/MANIFEST.MF:META-INF/MANIFEST.MF",
					"multi-release.dat:multi-release.dat",
					"META-INF/versions/%1$d/multi-release.dat:META-INF/versions/%1$d/multi-release.dat"
						.formatted(TestJar.MULTI_JAR_VERSION));
		}
	}

	@Test
	void versionedStreamStreamsEntries() throws IOException {
		try (NestedJarFile jar = new NestedJarFile(this.file, "multi-release.jar", Runtime.version())) {
			assertThat(jar.versionedStream().map((entry) -> entry.getName() + ":" + entry.getRealName()))
				.containsExactly("META-INF/:META-INF/", "META-INF/MANIFEST.MF:META-INF/MANIFEST.MF",
						"multi-release.dat:META-INF/versions/%1$d/multi-release.dat"
							.formatted(TestJar.MULTI_JAR_VERSION));
		}
	}

	@Test // gh-39166
	void getCommentAlignsWithJdkJar() throws Exception {
		File file = new File(this.tempDir, "testcomments.jar");
		try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(file))) {
			jar.putNextEntry(new ZipEntry("BOOT-INF/"));
			jar.closeEntry();
			jar.putNextEntry(new ZipEntry("BOOT-INF/classes/"));
			jar.closeEntry();
			for (int i = 0; i < 5; i++) {
				ZipEntry entry = new ZipEntry("BOOT-INF/classes/T" + i + ".class");
				entry.setComment("T" + i);
				jar.putNextEntry(entry);
				jar.write(UUID.randomUUID().toString().getBytes());
				jar.closeEntry();
			}
		}
		List<String> jdk = collectComments(new JarFile(file));
		List<String> nested = collectComments(new NestedJarFile(file, "BOOT-INF/classes/"));
		assertThat(nested).isEqualTo(jdk);
	}

	private List<String> collectComments(JarFile jarFile) throws IOException {
		try (jarFile) {
			List<String> comments = new ArrayList<>();
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				String comment = entries.nextElement().getComment();
				if (comment != null) {
					comments.add(comment);
				}
			}
			return comments;
		}
	}

}
