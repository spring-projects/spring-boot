/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.io.Layout;
import org.springframework.boot.buildpack.platform.io.Owner;

/**
 * A test {@link Buildpack}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class TestBuildpack implements Buildpack {

	private final BuildpackCoordinates coordinates;

	TestBuildpack(String id, @Nullable String version) {
		this.coordinates = BuildpackCoordinates.of(id, version);
	}

	@Override
	public BuildpackCoordinates getCoordinates() {
		return this.coordinates;
	}

	@Override
	public void apply(IOConsumer<Layer> layers) throws IOException {
		layers.accept(Layer.of(this::getContent));
	}

	private void getContent(Layout layout) throws IOException {
		String id = this.coordinates.getSanitizedId();
		String dir = "/cnb/buildpacks/" + id + "/" + this.coordinates.getVersion();
		layout.file(dir + "/buildpack.toml", Owner.ROOT, Content.of("[test]"));
	}

}
