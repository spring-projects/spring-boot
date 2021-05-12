/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

	@TempDir
	File temp;

	@Test
	void createWhenIndexFileIsEmptyThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new IndexedLayers(" \n "))
				.withMessage("Empty layer index file loaded");
	}

	@Test
	void createWhenIndexFileIsMalformedThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new IndexedLayers("test"))
				.withMessage("Layer index file is malformed");
	}

	@Test
	void iteratorReturnsLayers() throws Exception {
		IndexedLayers layers = new IndexedLayers(getIndex());
		assertThat(layers).containsExactly("test", "empty", "application");
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
	void getLayerWhenMatchesDirectoryReturnsLayer() throws Exception {
		IndexedLayers layers = new IndexedLayers(getIndex());
		assertThat(layers.getLayer(mockEntry("META-INF/MANIFEST.MF"))).isEqualTo("application");
		assertThat(layers.getLayer(mockEntry("META-INF/a/sub/directory/and/a/file"))).isEqualTo("application");
	}

	@Test
	void getLayerWhenFileHasSpaceReturnsLayer() throws Exception {
		IndexedLayers layers = new IndexedLayers(getIndex());
		assertThat(layers.getLayer(mockEntry("a b/c d"))).isEqualTo("application");
	}

	@Test
	void getShouldReturnIndexedLayersFromContext() throws Exception {
		Context context = mock(Context.class);
		given(context.getArchiveFile()).willReturn(createWarFile("test.war"));
		IndexedLayers layers = IndexedLayers.get(context);
		assertThat(layers.getLayer(mockEntry("WEB-INF/lib/a.jar"))).isEqualTo("test");
	}

	private String getIndex() throws Exception {
		return getFile("test-layers.idx");
	}

	private String getFile(String fileName) throws Exception {
		ClassPathResource resource = new ClassPathResource(fileName, getClass());
		InputStreamReader reader = new InputStreamReader(resource.getInputStream());
		return FileCopyUtils.copyToString(reader);
	}

	private ZipEntry mockEntry(String name) {
		ZipEntry entry = mock(ZipEntry.class);
		given(entry.getName()).willReturn(name);
		return entry;
	}

	private File createWarFile(String name) throws Exception {
		File file = new File(this.temp, name);
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
			out.putNextEntry(new ZipEntry("WEB-INF/lib/a/"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("WEB-INF/lib/a/a.jar"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("WEB-INF/classes/Demo.class"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			out.write(getFile("test-war-manifest.MF").getBytes());
			out.closeEntry();
			out.putNextEntry(new ZipEntry("WEB-INF/layers.idx"));
			out.write(getFile("test-war-layers.idx").getBytes());
			out.closeEntry();
		}
		return file;
	}

}
