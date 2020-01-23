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

/**
 * Tests for {@link ImplicitLayers}.
 *
 * @author Phillip Webb
 */
class ImplicitLayersTests {

	private Layers layers = new ImplicitLayers();

	@Test
	void iteratorReturnsLayers() {
		assertThat(this.layers).containsExactly("dependencies", "snapshot-dependencies", "resources", "application");
	}

	@Test
	void getLayerWhenSnapshotJarReturnsSnapshotDependencies() {
		assertThat(this.layers.getLayer(zipEntry("BOOT-INF/lib/mylib-SNAPSHOT.jar")))
				.isEqualTo("snapshot-dependencies");
	}

	@Test
	void getLayerWhenNonSnapshotJarReturnsDependencies() {
		assertThat(this.layers.getLayer(zipEntry("BOOT-INF/lib/mylib.jar"))).isEqualTo("dependencies");
	}

	@Test
	void getLayerWhenLoaderClassReturnsApplication() {
		assertThat(this.layers.getLayer(zipEntry("org/springframework/boot/loader/Example.class")))
				.isEqualTo("application");
	}

	@Test
	void getLayerWhenStaticResourceReturnsResources() {
		assertThat(this.layers.getLayer(zipEntry("BOOT-INF/classes/META-INF/resources/image.gif")))
				.isEqualTo("resources");
		assertThat(this.layers.getLayer(zipEntry("BOOT-INF/classes/resources/image.gif"))).isEqualTo("resources");
		assertThat(this.layers.getLayer(zipEntry("BOOT-INF/classes/static/image.gif"))).isEqualTo("resources");
		assertThat(this.layers.getLayer(zipEntry("BOOT-INF/classes/public/image.gif"))).isEqualTo("resources");
		assertThat(this.layers.getLayer(zipEntry("META-INF/resources/image.gif"))).isEqualTo("resources");
		assertThat(this.layers.getLayer(zipEntry("resources/image.gif"))).isEqualTo("resources");
		assertThat(this.layers.getLayer(zipEntry("static/image.gif"))).isEqualTo("resources");
		assertThat(this.layers.getLayer(zipEntry("public/image.gif"))).isEqualTo("resources");
	}

	@Test
	void getLayerWhenRegularClassReturnsApplication() {
		assertThat(this.layers.getLayer(zipEntry("BOOT-INF/classes/com.example/App.class"))).isEqualTo("application");
	}

	@Test
	void getLayerWhenClassResourceReturnsApplication() {
		assertThat(this.layers.getLayer(zipEntry("BOOT-INF/classes/application.properties"))).isEqualTo("application");
	}

	private ZipEntry zipEntry(String name) {
		return new ZipEntry(name);
	}

}
