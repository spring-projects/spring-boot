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

package org.springframework.boot.loader.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ImplicitLayerResolver}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ImplicitLayerResolverTests {

	private Layers layers = Layers.IMPLICIT;

	@Test
	void iteratorReturnsLayers() {
		assertThat(this.layers).containsExactly(StandardLayers.DEPENDENCIES, StandardLayers.SPRING_BOOT_LOADER,
				StandardLayers.SNAPSHOT_DEPENDENCIES, StandardLayers.APPLICATION);
	}

	@Test
	void getLayerWhenNameInResourceLocationReturnsApplicationLayer() {
		assertThat(this.layers.getLayer("META-INF/resources/logo.gif")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("resources/logo.gif")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("static/logo.gif")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("public/logo.gif")).isEqualTo(StandardLayers.APPLICATION);
	}

	@Test
	void getLayerWhenNameIsClassInResourceLocationReturnsApplicationLayer() {
		assertThat(this.layers.getLayer("META-INF/resources/Logo.class")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("resources/Logo.class")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("static/Logo.class")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("public/Logo.class")).isEqualTo(StandardLayers.APPLICATION);
	}

	@Test
	void getLayerWhenNameNotInResourceLocationReturnsApplicationLayer() {
		assertThat(this.layers.getLayer("com/example/Application.class")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("com/example/application.properties")).isEqualTo(StandardLayers.APPLICATION);
	}

	@Test
	void getLayerWhenLoaderClassReturnsLoaderLayer() {
		assertThat(this.layers.getLayer("org/springframework/boot/loader/Launcher.class"))
				.isEqualTo(StandardLayers.SPRING_BOOT_LOADER);
		assertThat(this.layers.getLayer("org/springframework/boot/loader/Utils.class"))
				.isEqualTo(StandardLayers.SPRING_BOOT_LOADER);
	}

	@Test
	void getLayerWhenLibraryIsSnapshotReturnsSnapshotLayer() {
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0.BUILD-SNAPSHOT.jar")))
				.isEqualTo(StandardLayers.SNAPSHOT_DEPENDENCIES);
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0-SNAPSHOT.jar")))
				.isEqualTo(StandardLayers.SNAPSHOT_DEPENDENCIES);
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0.SNAPSHOT.jar")))
				.isEqualTo(StandardLayers.SNAPSHOT_DEPENDENCIES);
	}

	@Test
	void getLayerWhenLibraryIsNotSnapshotReturnsDependenciesLayer() {
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0.jar"))).isEqualTo(StandardLayers.DEPENDENCIES);
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0-classified.jar")))
				.isEqualTo(StandardLayers.DEPENDENCIES);
	}

	private Library mockLibrary(String name) {
		Library library = mock(Library.class);
		given(library.getName()).willReturn(name);
		return library;
	}

}
