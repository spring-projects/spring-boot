/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.jar;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;
import org.springframework.boot.loader.zip.ZipContent;
import org.springframework.boot.loader.zip.ZipContent.Entry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityInfo}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class SecurityInfoTests {

	@TempDir
	File temp;

	@Test
	void getWhenNoSignatureFileReturnsNone() throws Exception {
		File file = new File(this.temp, "test.jar");
		TestJar.create(file);
		try (ZipContent content = ZipContent.open(file.toPath())) {
			SecurityInfo info = SecurityInfo.get(content);
			assertThat(info).isSameAs(SecurityInfo.NONE);
			for (int i = 0; i < content.size(); i++) {
				Entry entry = content.getEntry(i);
				assertThat(info.getCertificates(entry)).isNull();
				assertThat(info.getCodeSigners(entry)).isNull();
			}
		}
	}

	@Test
	void getWhenHasSignatureFileButNoSecurityMaterialReturnsNone() throws Exception {
		File file = new File(this.temp, "test.jar");
		TestJar.create(file, false, "DSA");
		try (ZipContent content = ZipContent.open(file.toPath())) {
			assertThat(content.hasJarSignatureFile()).isTrue();
			SecurityInfo info = SecurityInfo.get(content);
			assertThat(info).isSameAs(SecurityInfo.NONE);
		}
	}

	@Test
	void getWhenJarIsSigned() throws Exception {
		File file = TestJar.getSigned();
		try (ZipContent content = ZipContent.open(file.toPath())) {
			assertThat(content.hasJarSignatureFile()).isTrue();
			SecurityInfo info = SecurityInfo.get(content);
			for (int i = 0; i < content.size(); i++) {
				Entry entry = content.getEntry(i);
				if (entry.getName().endsWith(".class")) {
					assertThat(info.getCertificates(entry)).isNotNull();
					assertThat(info.getCodeSigners(entry)).isNotNull();
				}
			}
		}
	}

}
