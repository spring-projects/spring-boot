/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.jarmode.layertools;

import java.io.InputStreamReader;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IndexedLayers}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class IndexedLayersTests {

	@Test
	void createWhenIndexFileIsEmptyThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new IndexedLayers(" \n "))
				.withMessage("Empty layer index file loaded");
	}

	@Test
	void createWhenIndexFileIsMalformedThrowsException() throws Exception {
		assertThatIllegalStateException().isThrownBy(() -> new IndexedLayers("test"))
				.withMessage("Layer index file is malformed");
	}

	@Test
	void iteratorReturnsLayers() throws Exception {
		IndexedLayers layers = new IndexedLayers(getIndex());
		assertThat(layers).containsExactly("test", "application");
	}

	@Test
	void getLayerWhenMatchesNameReturnsLayer() throws Exception {
		IndexedLayers layers = new IndexedLayers(getIndex());
		assertThat(layers.getLayer(mockEntry("BOOT-INF/lib/a.jar"))).isEqualTo("test");
		assertThat(layers.getLayer(mockEntry("BOOT-INF/classes/Demo.class"))).isEqualTo("application");
	}

	@Test
	void getLayerWhenMatchesNameForMissingLayerThrowsException() throws Exception {
		IndexedLayers layers = new IndexedLayers(getIndex());
		assertThatIllegalStateException().isThrownBy(() -> layers.getLayer(mockEntry("file.jar")))
				.withMessage("No layer defined in index for file " + "'file.jar'");
	}

	@Test
	void getLayerWhenMatchesFolderReturnsLayer() throws Exception {
		IndexedLayers layers = new IndexedLayers(getIndex());
		assertThat(layers.getLayer(mockEntry("META-INF/MANIFEST.MF"))).isEqualTo("application");
		assertThat(layers.getLayer(mockEntry("META-INF/a/sub/folder/and/a/file"))).isEqualTo("application");
	}

	@Test
	void getLayerWhenFileHasSpaceReturnsLayer() throws Exception {
		IndexedLayers layers = new IndexedLayers(getIndex());
		assertThat(layers.getLayer(mockEntry("a b/c d"))).isEqualTo("application");
	}

	private String getIndex() throws Exception {
		ClassPathResource resource = new ClassPathResource("test-layers.idx", getClass());
		InputStreamReader reader = new InputStreamReader(resource.getInputStream());
		return FileCopyUtils.copyToString(reader);
	}

	private ZipEntry mockEntry(String name) {
		ZipEntry entry = mock(ZipEntry.class);
		given(entry.getName()).willReturn(name);
		return entry;
	}

}
