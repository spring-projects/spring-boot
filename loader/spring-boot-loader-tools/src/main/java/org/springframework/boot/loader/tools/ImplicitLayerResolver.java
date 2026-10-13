/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

/**
 * Implementation of {@link Layers} that uses implicit rules.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ImplicitLayerResolver extends StandardLayers {

	private static final String SPRING_BOOT_LOADER_PREFIX = "org/springframework/boot/loader/";

	@Override
	public Layer getLayer(String name) {
		if (name.startsWith(SPRING_BOOT_LOADER_PREFIX)) {
			return SPRING_BOOT_LOADER;
		}
		return APPLICATION;
	}

	@Override
	public Layer getLayer(Library library) {
		if (library.isLocal()) {
			return APPLICATION;
		}
		if (library.getName().contains("SNAPSHOT.")) {
			return SNAPSHOT_DEPENDENCIES;
		}
		return DEPENDENCIES;
	}

}
