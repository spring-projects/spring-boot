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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;

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

	TestBuildpack(String id, String version) {
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
