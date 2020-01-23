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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * {@link Layers} implementation backed by a {@code BOOT-INF/layers.idx} file.
 *
 * @author Phillip Webb
 */
class IndexedLayers implements Layers {

	private static final String APPLICATION_LAYER = "application";

	private static final String SPRING_BOOT_APPLICATION_LAYER = "springbootapplication";

	private static final Pattern LAYER_PATTERN = Pattern.compile("^BOOT-INF\\/layers\\/([a-zA-Z0-9-]+)\\/.*$");

	private List<String> layers;

	IndexedLayers(String indexFile) {
		String[] lines = indexFile.split("\n");
		this.layers = Arrays.stream(lines).map(String::trim).filter((line) -> !line.isEmpty())
				.collect(Collectors.toCollection(ArrayList::new));
		Assert.state(!this.layers.isEmpty(), "Empty layer index file loaded");
		if (!this.layers.contains(APPLICATION_LAYER)) {
			this.layers.add(0, SPRING_BOOT_APPLICATION_LAYER);
		}
	}

	@Override
	public Iterator<String> iterator() {
		return this.layers.iterator();
	}

	@Override
	public String getLayer(ZipEntry entry) {
		String name = entry.getName();
		Matcher matcher = LAYER_PATTERN.matcher(name);
		if (matcher.matches()) {
			String layer = matcher.group(1);
			Assert.state(this.layers.contains(layer), "Unexpected layer '" + layer + "'");
			return layer;
		}
		return this.layers.contains(APPLICATION_LAYER) ? APPLICATION_LAYER : SPRING_BOOT_APPLICATION_LAYER;
	}

	/**
	 * Get an {@link IndexedLayers} instance of possible.
	 * @param context the context
	 * @return an {@link IndexedLayers} instance or {@code null} if this not a layered
	 * jar.
	 */
	static IndexedLayers get(Context context) {
		try {
			try (JarFile jarFile = new JarFile(context.getJarFile())) {
				ZipEntry entry = jarFile.getEntry("BOOT-INF/layers.idx");
				if (entry != null) {
					String indexFile = StreamUtils.copyToString(jarFile.getInputStream(entry), StandardCharsets.UTF_8);
					return new IndexedLayers(indexFile);
				}
			}
			return null;
		}
		catch (FileNotFoundException | NoSuchFileException ex) {
			return null;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
