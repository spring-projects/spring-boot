/*
 * Copyright 2012-present the original author or authors.
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

import org.yaml.snakeyaml.LoaderOptions;
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
		return stream().filter((entry) -> entry.containsKey(layerName))
			.findFirst()
			.map((entry) -> entry.get(layerName))
			.orElse(Collections.emptyList());
	}

	static LayersIndex fromArchiveFile(File archiveFile) throws IOException {
		String indexPath = (archiveFile.getName().endsWith(".war") ? "WEB-INF/layers.idx" : "BOOT-INF/layers.idx");
		try (JarFile jarFile = new JarFile(archiveFile)) {
			ZipEntry indexEntry = jarFile.getEntry(indexPath);
			Yaml yaml = new Yaml(new Constructor(LayersIndex.class, getLoaderOptions()));
			return yaml.load(jarFile.getInputStream(indexEntry));
		}
	}

	private static LoaderOptions getLoaderOptions() {
		LoaderOptions loaderOptions = new LoaderOptions();
		loaderOptions.setAllowDuplicateKeys(false);
		loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
		loaderOptions.setAllowRecursiveKeys(true);
		return loaderOptions;
	}

}
