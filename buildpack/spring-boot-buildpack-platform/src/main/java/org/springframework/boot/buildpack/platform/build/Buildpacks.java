/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

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

	static Buildpacks of(@Nullable List<Buildpack> buildpacks) {
		return CollectionUtils.isEmpty(buildpacks) ? EMPTY : new Buildpacks(buildpacks);
	}

}
