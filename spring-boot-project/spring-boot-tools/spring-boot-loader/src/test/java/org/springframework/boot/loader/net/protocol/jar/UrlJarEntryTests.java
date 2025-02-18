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

package org.springframework.boot.loader.net.protocol.jar;

import java.util.jar.Attributes;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UrlJarEntry}.
 *
 * @author Phillip Webb
 */
class UrlJarEntryTests {

	@Test
	void ofWhenEntryIsNullReturnsNull() {
		assertThat(UrlJarEntry.of(null, null)).isNull();
	}

	@Test
	void ofReturnsUrlJarEntry() {
		JarEntry entry = new JarEntry("test");
		assertThat(UrlJarEntry.of(entry, null)).isNotNull();

	}

	@Test
	void getAttributesDelegatesToUrlJarManifest() throws Exception {
		JarEntry entry = new JarEntry("test");
		UrlJarManifest manifest = mock(UrlJarManifest.class);
		Attributes attributes = mock(Attributes.class);
		given(manifest.getEntryAttributes(any())).willReturn(attributes);
		assertThat(UrlJarEntry.of(entry, manifest).getAttributes()).isSameAs(attributes);
	}

}
