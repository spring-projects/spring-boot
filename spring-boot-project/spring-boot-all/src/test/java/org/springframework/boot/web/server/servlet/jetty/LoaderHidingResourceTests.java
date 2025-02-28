/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.web.server.servlet.jetty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.eclipse.jetty.util.resource.PathResourceFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoaderHidingResource}.
 *
 * @author Andy Wilkinson
 */
class LoaderHidingResourceTests {

	@Test
	void listHidesLoaderResources(@TempDir File temp) throws IOException {
		URI warUri = createExampleWar(temp);
		Resource resource = new PathResourceFactory().newResource(warUri);
		LoaderHidingResource loaderHidingResource = new LoaderHidingResource(resource, resource);
		assertThat(deepList(loaderHidingResource)).hasOnlyElementsOfType(LoaderHidingResource.class)
			.extracting(Resource::getName)
			.contains("/assets/image.jpg")
			.doesNotContain("/org/springframework/boot/Loader.class");
	}

	@Test
	void getAllResourcesHidesLoaderResources(@TempDir File temp) throws IOException {
		URI warUri = createExampleWar(temp);
		Resource resource = new PathResourceFactory().newResource(warUri);
		LoaderHidingResource loaderHidingResource = new LoaderHidingResource(resource, resource);
		Collection<Resource> allResources = loaderHidingResource.getAllResources();
		assertThat(allResources).hasOnlyElementsOfType(LoaderHidingResource.class)
			.extracting(Resource::getName)
			.contains("/assets/image.jpg")
			.doesNotContain("/org/springframework/boot/Loader.class");
	}

	@Test
	void resolveHidesLoaderResources(@TempDir File temp) throws IOException {
		URI warUri = createExampleWar(temp);
		Resource resource = new PathResourceFactory().newResource(warUri);
		LoaderHidingResource loaderHidingResource = new LoaderHidingResource(resource, resource);
		assertThat(loaderHidingResource.resolve("/assets/image.jpg").exists()).isTrue();
		assertThat(loaderHidingResource.resolve("/assets/image.jpg")).isInstanceOf(LoaderHidingResource.class);
		assertThat(loaderHidingResource.resolve("/assets/non-existent.jpg").exists()).isFalse();
		assertThat(loaderHidingResource.resolve("/assets/non-existent.jpg")).isInstanceOf(LoaderHidingResource.class);
		assertThat(loaderHidingResource.resolve("/org/springframework/boot/Loader.class")).isNull();
	}

	private URI createExampleWar(File temp) throws IOException {
		File exampleWarFile = new File(temp, "example.war");
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(exampleWarFile))) {
			out.putNextEntry(new ZipEntry("org/"));
			out.putNextEntry(new ZipEntry("org/springframework/"));
			out.putNextEntry(new ZipEntry("org/springframework/boot/"));
			out.putNextEntry(new ZipEntry("org/springframework/boot/Loader.class"));
			out.putNextEntry(new ZipEntry("assets/"));
			out.putNextEntry(new ZipEntry("assets/image.jpg"));
		}
		URI warUri = URI.create("jar:" + exampleWarFile.toURI() + "!/");
		FileSystems.newFileSystem(warUri, Collections.emptyMap());
		return warUri;
	}

	private List<Resource> deepList(Resource resource) {
		List<Resource> all = new ArrayList<>();
		for (Resource listed : resource.list()) {
			all.add(listed);
			all.addAll(deepList(listed));
		}
		return all;
	}

}
