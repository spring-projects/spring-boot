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

package org.springframework.boot.loader.jar;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

import org.springframework.boot.loader.jar.JarURLConnection.JarEntryName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarEntryName}.
 *
 * @author Andy Wilkinson
 */
public class JarEntryNameTests {

	@Test
	public void basicName() {
		assertThat(new JarEntryName("a/b/C.class").toString()).isEqualTo("a/b/C.class");
	}

	@Test
	public void nameWithSingleByteEncodedCharacters() {
		assertThat(new JarEntryName("%61/%62/%43.class").toString())
				.isEqualTo("a/b/C.class");
	}

	@Test
	public void nameWithDoubleByteEncodedCharacters() {
		assertThat(new JarEntryName("%c3%a1/b/C.class").toString())
				.isEqualTo("\u00e1/b/C.class");
	}

	@Test
	public void nameWithMixtureOfEncodedAndUnencodedDoubleByteCharacters()
			throws UnsupportedEncodingException {
		assertThat(new JarEntryName("%c3%a1/b/\u00c7.class").toString())
				.isEqualTo("\u00e1/b/\u00c7.class");
	}

}
