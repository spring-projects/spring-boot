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

package org.springframework.boot.image.paketo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Index file describing the layers in the jar or war file and the files or directories in
 * each layer.
 *
 * @author Scott Frederick
 */
class LayersIndex extends ArrayList<Map<String, List<String>>> {

	List<String> getLayer(String layerName) {
		return stream().filter((entry) -> entry.containsKey(layerName)).findFirst().map((entry) -> entry.get(layerName))
				.orElse(Collections.emptyList());
	}

	static LayersIndex fromArchiveFile(File archiveFile) throws IOException {
		String indexPath = (archiveFile.getName().endsWith(".war") ? "WEB-INF/layers.idx" : "BOOT-INF/layers.idx");
		try (JarFile jarFile = new JarFile(archiveFile)) {
			ZipEntry indexEntry = jarFile.getEntry(indexPath);
			Yaml yaml = new Yaml(new Constructor(LayersIndex.class));
			return yaml.load(jarFile.getInputStream(indexEntry));
		}
	}

}
