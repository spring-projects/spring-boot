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

package org.springframework.boot.maven;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCoordinates;
import org.springframework.boot.loader.tools.layer.CustomLayers;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CustomLayersProvider}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 */
public class CustomLayersProviderTests {

	private CustomLayersProvider customLayersProvider;

	@BeforeEach
	void setup() {
		this.customLayersProvider = new CustomLayersProvider();
	}

	@Test
	void getLayerResolverWhenDocumentValid() throws Exception {
		CustomLayers layers = this.customLayersProvider.getLayers(getDocument("layers.xml"));
		assertThat(layers).extracting("name").containsExactly("my-deps", "my-dependencies-name",
				"snapshot-dependencies", "my-resources", "configuration", "application");
		Library snapshot = mockLibrary("test-SNAPSHOT.jar", "org.foo", "1.0.0-SNAPSHOT");
		Library groupId = mockLibrary("my-library", "com.acme", null);
		Library otherDependency = mockLibrary("other-library", "org.foo", null);
		Library localSnapshotDependency = mockLibrary("local-library", "org.foo", "1.0-SNAPSHOT");
		given(localSnapshotDependency.isLocal()).willReturn(true);
		assertThat(layers.getLayer(snapshot).toString()).isEqualTo("snapshot-dependencies");
		assertThat(layers.getLayer(groupId).toString()).isEqualTo("my-deps");
		assertThat(layers.getLayer(otherDependency).toString()).isEqualTo("my-dependencies-name");
		assertThat(layers.getLayer(localSnapshotDependency).toString()).isEqualTo("application");
		assertThat(layers.getLayer("META-INF/resources/test.css").toString()).isEqualTo("my-resources");
		assertThat(layers.getLayer("application.yml").toString()).isEqualTo("configuration");
		assertThat(layers.getLayer("test").toString()).isEqualTo("application");
	}

	private Library mockLibrary(String name, String groupId, String version) {
		Library library = mock(Library.class);
		given(library.getName()).willReturn(name);
		given(library.getCoordinates()).willReturn(LibraryCoordinates.of(groupId, null, version));
		return library;
	}

	@Test
	void getLayerResolverWhenDocumentContainsLibraryLayerWithNoFilters() throws Exception {
		CustomLayers layers = this.customLayersProvider.getLayers(getDocument("dependencies-layer-no-filter.xml"));
		Library library = mockLibrary("my-library", "com.acme", null);
		assertThat(layers.getLayer(library).toString()).isEqualTo("my-deps");
		assertThatIllegalStateException().isThrownBy(() -> layers.getLayer("application.yml"))
				.withMessageContaining("match any layer");
	}

	@Test
	void getLayerResolverWhenDocumentContainsResourceLayerWithNoFilters() throws Exception {
		CustomLayers layers = this.customLayersProvider.getLayers(getDocument("application-layer-no-filter.xml"));
		Library library = mockLibrary("my-library", "com.acme", null);
		assertThat(layers.getLayer("application.yml").toString()).isEqualTo("my-layer");
		assertThatIllegalStateException().isThrownBy(() -> layers.getLayer(library))
				.withMessageContaining("match any layer");
	}

	private Document getDocument(String resourceName) throws Exception {
		ClassPathResource resource = new ClassPathResource(resourceName);
		InputSource inputSource = new InputSource(resource.getInputStream());
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		return documentBuilder.parse(inputSource);
	}

}
