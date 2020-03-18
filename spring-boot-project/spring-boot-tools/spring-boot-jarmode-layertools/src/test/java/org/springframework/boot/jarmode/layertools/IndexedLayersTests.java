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

import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IndexedLayers}.
 *
 * @author Phillip Webb
 */
class IndexedLayersTests {

	@Test
	void createWhenIndexFileIsEmptyThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new IndexedLayers(" \n "))
				.withMessage("Empty layer index file loaded");
	}

	@Test
	void createWhenIndexFileHasNoApplicationLayerAddSpringBootApplication() {
		IndexedLayers layers = new IndexedLayers("test");
		assertThat(layers).contains("springbootapplication");
	}

	@Test
	void iteratorReturnsLayers() {
		IndexedLayers layers = new IndexedLayers("test\napplication");
		assertThat(layers).containsExactly("test", "application");
	}

	@Test
	void getLayerWhenMatchesLayerPatterReturnsLayer() {
		IndexedLayers layers = new IndexedLayers("test");
		assertThat(layers.getLayer(mockEntry("BOOT-INF/layers/test/lib/file.jar"))).isEqualTo("test");
	}

	@Test
	void getLayerWhenMatchesLayerPatterForMissingLayerThrowsException() {
		IndexedLayers layers = new IndexedLayers("test");
		assertThatIllegalStateException()
				.isThrownBy(() -> layers.getLayer(mockEntry("BOOT-INF/layers/missing/lib/file.jar")))
				.withMessage("Unexpected layer 'missing'");
	}

	@Test
	void getLayerWhenDoesNotMatchLayerPatternReturnsApplication() {
		IndexedLayers layers = new IndexedLayers("test\napplication");
		assertThat(layers.getLayer(mockEntry("META-INF/MANIFEST.MF"))).isEqualTo("application");
	}

	@Test
	void getLayerWhenDoesNotMatchLayerPatternAndHasNoApplicationLayerReturnsSpringApplication() {
		IndexedLayers layers = new IndexedLayers("test");
		assertThat(layers.getLayer(mockEntry("META-INF/MANIFEST.MF"))).isEqualTo("springbootapplication");
	}

	private ZipEntry mockEntry(String name) {
		ZipEntry entry = mock(ZipEntry.class);
		given(entry.getName()).willReturn(name);
		return entry;
	}

}
