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
import java.util.Collections;
import java.util.List;

import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.io.Layout;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A collection of {@link Buildpack} instances that can be used to apply buildpack layers.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class Buildpacks {

	static final Buildpacks EMPTY = new Buildpacks(Collections.emptyList());

	private final List<Buildpack> buildpacks;

	private Buildpacks(List<Buildpack> buildpacks) {
		this.buildpacks = buildpacks;
	}

	List<Buildpack> getBuildpacks() {
		return this.buildpacks;
	}

	void apply(IOConsumer<Layer> layers) throws IOException {
		if (!this.buildpacks.isEmpty()) {
			for (Buildpack buildpack : this.buildpacks) {
				buildpack.apply(layers);
			}
			layers.accept(Layer.of(this::addOrderLayerContent));
		}
	}

	void addOrderLayerContent(Layout layout) throws IOException {
		layout.file("/cnb/order.toml", Owner.ROOT, Content.of(getOrderToml()));
	}

	private String getOrderToml() {
		StringBuilder builder = new StringBuilder();
		builder.append("[[order]]\n\n");
		for (Buildpack buildpack : this.buildpacks) {
			appendToOrderToml(builder, buildpack.getCoordinates());
		}
		return builder.toString();
	}

	private void appendToOrderToml(StringBuilder builder, BuildpackCoordinates coordinates) {
		builder.append("  [[order.group]]\n");
		builder.append("    id = \"" + coordinates.getId() + "\"\n");
		if (StringUtils.hasText(coordinates.getVersion())) {
			builder.append("    version = \"" + coordinates.getVersion() + "\"\n");
		}
		builder.append("\n");
	}

	static Buildpacks of(List<Buildpack> buildpacks) {
		return CollectionUtils.isEmpty(buildpacks) ? EMPTY : new Buildpacks(buildpacks);
	}

}
