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

package org.springframework.boot.loader.tools.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCoordinates;
import org.springframework.boot.loader.tools.layer.application.FilteredResourceStrategy;
import org.springframework.boot.loader.tools.layer.application.LocationFilter;
import org.springframework.boot.loader.tools.layer.library.CoordinateFilter;
import org.springframework.boot.loader.tools.layer.library.FilteredLibraryStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CustomLayers}.
 *
 * @author Stephane Nicoll
 */
class CustomLayersTests {

	@Test
	void customLayersAreAvailable() {
		Layer first = new Layer("first");
		Layer second = new Layer("second");
		CustomLayers customLayers = new CustomLayers(Arrays.asList(first, second), Collections.emptyList(),
				Collections.emptyList());
		List<Layer> actualLayers = new ArrayList<>();
		customLayers.iterator().forEachRemaining(actualLayers::add);
		assertThat(actualLayers).containsExactly(first, second);
	}

	@Test
	void layerForResourceIsFound() {
		FilteredResourceStrategy resourceStrategy = new FilteredResourceStrategy("test", Collections
				.singletonList(new LocationFilter(Collections.singletonList("META-INF/**"), Collections.emptyList())));
		Layer targetLayer = new Layer("test");
		CustomLayers customLayers = new CustomLayers(Collections.singletonList(targetLayer),
				Collections.singletonList(resourceStrategy), Collections.emptyList());
		assertThat(customLayers.getLayer("META-INF/manifest.mf")).isNotNull().isEqualTo(targetLayer);
	}

	@Test
	void layerForResourceIsNotFound() {
		FilteredResourceStrategy resourceStrategy = new FilteredResourceStrategy("test", Collections
				.singletonList(new LocationFilter(Collections.singletonList("META-INF/**"), Collections.emptyList())));
		CustomLayers customLayers = new CustomLayers(Collections.singletonList(new Layer("test")),
				Collections.singletonList(resourceStrategy), Collections.emptyList());
		assertThatIllegalStateException().isThrownBy(() -> customLayers.getLayer("com/acme"));
	}

	@Test
	void layerForResourceIsNotInListedLayers() {
		FilteredResourceStrategy resourceStrategy = new FilteredResourceStrategy("test-not-listed", Collections
				.singletonList(new LocationFilter(Collections.singletonList("META-INF/**"), Collections.emptyList())));
		Layer targetLayer = new Layer("test");
		CustomLayers customLayers = new CustomLayers(Collections.singletonList(targetLayer),
				Collections.singletonList(resourceStrategy), Collections.emptyList());
		assertThatIllegalStateException().isThrownBy(() -> customLayers.getLayer("META-INF/manifest.mf"))
				.withMessageContaining("META-INF/manifest.mf").withMessageContaining("test-not-listed")
				.withMessageContaining("[test]");
	}

	@Test
	void layerForLibraryIsFound() {
		FilteredLibraryStrategy libraryStrategy = new FilteredLibraryStrategy("test", Collections
				.singletonList(new CoordinateFilter(Collections.singletonList("com.acme:*"), Collections.emptyList())));
		Layer targetLayer = new Layer("test");
		CustomLayers customLayers = new CustomLayers(Collections.singletonList(targetLayer), Collections.emptyList(),
				Collections.singletonList(libraryStrategy));
		assertThat(customLayers.getLayer(mockLibrary("com.acme:test"))).isNotNull().isEqualTo(targetLayer);
	}

	@Test
	void layerForLibraryIsNotFound() {
		FilteredLibraryStrategy libraryStrategy = new FilteredLibraryStrategy("test", Collections
				.singletonList(new CoordinateFilter(Collections.singletonList("com.acme:*"), Collections.emptyList())));
		CustomLayers customLayers = new CustomLayers(Collections.singletonList(new Layer("test")),
				Collections.emptyList(), Collections.singletonList(libraryStrategy));
		assertThatIllegalStateException().isThrownBy(() -> customLayers.getLayer(mockLibrary("org.another:test")));
	}

	@Test
	void layerForLibraryIsNotInListedLayers() {
		FilteredLibraryStrategy libraryStrategy = new FilteredLibraryStrategy("test-not-listed", Collections
				.singletonList(new CoordinateFilter(Collections.singletonList("com.acme:*"), Collections.emptyList())));
		Layer targetLayer = new Layer("test");
		CustomLayers customLayers = new CustomLayers(Collections.singletonList(targetLayer), Collections.emptyList(),
				Collections.singletonList(libraryStrategy));
		assertThatIllegalStateException().isThrownBy(() -> customLayers.getLayer(mockLibrary("com.acme:test")))
				.withMessageContaining("com.acme:test").withMessageContaining("test-not-listed")
				.withMessageContaining("[test]");
	}

	private Library mockLibrary(String coordinates) {
		Library library = mock(Library.class);
		given(library.getCoordinates()).willReturn(new LibraryCoordinates(coordinates));
		given(library.getName()).willReturn(coordinates);
		return library;
	}

}
