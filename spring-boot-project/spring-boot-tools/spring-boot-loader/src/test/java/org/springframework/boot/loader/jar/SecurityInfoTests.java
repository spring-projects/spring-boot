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
	void getWhenHasSignatureFileButNoSecuityMaterialReturnsNone() throws Exception {
		File file = new File(this.temp, "test.jar");
		TestJar.create(file, false, true);
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
