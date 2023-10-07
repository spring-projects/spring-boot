/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.launch;

import java.io.File;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.launch.Archive.Entry;
import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link Archive}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class ArchiveTests {

	@TempDir
	File temp;

	@Test
	void getClassPathUrlsWithOnlyIncludeFilterSearchesAllDirectories() throws Exception {
		Archive archive = mock(Archive.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
		Predicate<Entry> includeFilter = (entry) -> false;
		archive.getClassPathUrls(includeFilter);
		then(archive).should().getClassPathUrls(includeFilter, Archive.ALL_ENTRIES);
	}

	@Test
	void isExplodedWhenHasRootDirectoryReturnsTrue() {
		Archive archive = mock(Archive.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
		given(archive.getRootDirectory()).willReturn(this.temp);
		assertThat(archive.isExploded()).isTrue();
	}

	@Test
	void isExplodedWhenHasNoRootDirectoryReturnsFalse() {
		Archive archive = mock(Archive.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
		given(archive.getRootDirectory()).willReturn(null);
		assertThat(archive.isExploded()).isFalse();
	}

	@Test
	void createFromProtectionDomainCreatesJarArchive() throws Exception {
		File jarFile = new File(this.temp, "test.jar");
		TestJar.create(jarFile);
		ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
		CodeSource codeSource = mock(CodeSource.class);
		given(protectionDomain.getCodeSource()).willReturn(codeSource);
		given(codeSource.getLocation()).willReturn(jarFile.toURI().toURL());
		try (Archive archive = Archive.create(protectionDomain)) {
			assertThat(archive).isInstanceOf(JarFileArchive.class);
		}
	}

	@Test
	void createFromProtectionDomainWhenNoLocationThrowsException() throws Exception {
		File jarFile = new File(this.temp, "test.jar");
		TestJar.create(jarFile);
		ProtectionDomain protectionDomain = mock(ProtectionDomain.class);
		assertThatIllegalStateException().isThrownBy(() -> Archive.create(protectionDomain))
			.withMessage("Unable to determine code source archive");
	}

	@Test
	void createFromFileWhenFileDoesNotExistThrowsException() {
		File target = new File(this.temp, "missing");
		assertThatIllegalStateException().isThrownBy(() -> Archive.create(target))
			.withMessageContaining("Unable to determine code source archive");
	}

	@Test
	void createFromFileWhenJarFileReturnsJarFileArchive() throws Exception {
		File target = new File(this.temp, "missing");
		TestJar.create(target);
		try (Archive archive = Archive.create(target)) {
			assertThat(archive).isInstanceOf(JarFileArchive.class);
		}
	}

	@Test
	void createFromFileWhenDirectoryReturnsExplodedFileArchive() throws Exception {
		File target = this.temp;
		try (Archive archive = Archive.create(target)) {
			assertThat(archive).isInstanceOf(ExplodedArchive.class);
		}
	}

}
