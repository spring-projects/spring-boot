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

package org.springframework.boot.image.assertions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.test.json.JsonContentAssert;

/**
 * AssertJ {@link org.assertj.core.api.Assert} for Docker image contents.
 *
 * @author Scott Frederick
 */
public class ImageAssert extends AbstractAssert<ImageAssert, ImageReference> {

	private final HashMap<String, Layer> layers = new HashMap<>();

	ImageAssert(ImageReference imageReference) throws IOException {
		super(imageReference, ImageAssert.class);
		getLayers();
	}

	public void layer(String layerDigest, Consumer<LayerContentAssert> assertConsumer) {
		if (!this.layers.containsKey(layerDigest)) {
			failWithMessage("Layer with digest '%s' not found in image", layerDigest);
		}
		assertConsumer.accept(new LayerContentAssert(this.layers.get(layerDigest)));
	}

	private void getLayers() throws IOException {
		new DockerApi().image().exportLayers(this.actual, (id, tarArchive) -> {
			Layer layer = Layer.fromTarArchive(tarArchive);
			this.layers.put(layer.getId().toString(), layer);
		});
	}

	/**
	 * Asserts for image layers.
	 */
	public static class LayerContentAssert extends AbstractAssert<LayerContentAssert, Layer> {

		public LayerContentAssert(Layer layer) {
			super(layer, LayerContentAssert.class);
		}

		public ListAssert<String> entries() {
			List<String> entryNames = new ArrayList<>();
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				this.actual.writeTo(out);
				try (TarArchiveInputStream in = new TarArchiveInputStream(
						new ByteArrayInputStream(out.toByteArray()))) {
					TarArchiveEntry entry = in.getNextTarEntry();
					while (entry != null) {
						if (!entry.isDirectory()) {
							entryNames.add(entry.getName().replaceFirst("^/workspace/", ""));
						}
						entry = in.getNextTarEntry();
					}
				}
			}
			catch (IOException ex) {
				failWithMessage("IOException while reading image layer archive: '%s'", ex.getMessage());
			}
			return Assertions.assertThat(entryNames);
		}

		public void jsonEntry(String name, Consumer<JsonContentAssert> assertConsumer) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				this.actual.writeTo(out);
				try (TarArchiveInputStream in = new TarArchiveInputStream(
						new ByteArrayInputStream(out.toByteArray()))) {
					TarArchiveEntry entry = in.getNextTarEntry();
					while (entry != null) {
						if (entry.getName().equals(name)) {
							ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
							IOUtils.copy(in, entryOut);
							assertConsumer.accept(new JsonContentAssert(LayerContentAssert.class, entryOut.toString()));
							return;
						}
						entry = in.getNextTarEntry();
					}
				}
				failWithMessage("Expected JSON entry '%s' in layer with digest '%s'", name, this.actual.getId());
			}
			catch (IOException ex) {
				failWithMessage("IOException while reading image layer archive: '%s'", ex.getMessage());
			}
		}

	}

}
