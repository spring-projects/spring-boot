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

package org.springframework.boot.layertools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 * {@link Layers} implementation that uses implicit rules to slice the application.
 *
 * @author Phillip Webb
 */
class ImplicitLayers implements Layers {

	private static final String DEPENDENCIES_LAYER = "dependencies";

	private static final String SNAPSHOT_DEPENDENCIES_LAYER = "snapshot-dependencies";

	private static final String RESOURCES_LAYER = "resources";

	private static final String APPLICATION_LAYER = "application";

	private static final List<String> LAYERS;
	static {
		List<String> layers = new ArrayList<>();
		layers.add(DEPENDENCIES_LAYER);
		layers.add(SNAPSHOT_DEPENDENCIES_LAYER);
		layers.add(RESOURCES_LAYER);
		layers.add(APPLICATION_LAYER);
		LAYERS = Collections.unmodifiableList(layers);
	}

	private static final String[] CLASS_LOCATIONS = { "", "BOOT-INF/classes/" };

	private static final String[] RESOURCE_LOCATIONS = { "META-INF/resources/", "resources/", "static/", "public/" };

	@Override
	public Iterator<String> iterator() {
		return LAYERS.iterator();
	}

	@Override
	public String getLayer(ZipEntry entry) {
		return getLayer(entry.getName());
	}

	String getLayer(String name) {
		if (name.endsWith("SNAPSHOT.jar")) {
			return SNAPSHOT_DEPENDENCIES_LAYER;
		}
		if (name.endsWith(".jar")) {
			return DEPENDENCIES_LAYER;
		}
		if (!name.endsWith(".class")) {
			for (String classLocation : CLASS_LOCATIONS) {
				for (String resourceLocation : RESOURCE_LOCATIONS) {
					if (name.startsWith(classLocation + resourceLocation)) {
						return RESOURCES_LAYER;
					}
				}
			}
		}
		return APPLICATION_LAYER;
	}

}
