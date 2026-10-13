/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The OS distribution of an image, as described by the target data of the CNB platform
 * specification.
 *
 * @author Moritz Halbritter
 */
final class Distro {

	private static final String NAME_LABEL = "io.buildpacks.base.distro.name";

	private static final String VERSION_LABEL = "io.buildpacks.base.distro.version";

	// Paketo images only set these non-spec labels
	private static final String STACK_NAME_LABEL = "io.buildpacks.stack.distro.name";

	private static final String STACK_VERSION_LABEL = "io.buildpacks.stack.distro.version";

	private final @Nullable String name;

	private final @Nullable String version;

	private Distro(@Nullable String name, @Nullable String version) {
		this.name = name;
		this.version = version;
	}

	/**
	 * Return whether this distribution matches the given distribution. A missing name or
	 * version matches any value.
	 * @param other the distribution to compare with
	 * @return {@code true} if the distributions match
	 */
	boolean matches(Distro other) {
		return matches(this.name, other.name) && matches(this.version, other.version);
	}

	private static boolean matches(@Nullable String value, @Nullable String other) {
		if (value == null || other == null) {
			return true;
		}
		return value.equals(other);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (this.name != null) {
			result.append(this.name);
		}
		if (this.version != null) {
			if (!result.isEmpty()) {
				result.append(" ");
			}
			result.append(this.version);
		}
		return result.toString();
	}

	/**
	 * Factory method to create a {@link Distro} from an {@link Image}.
	 * @param image the source image
	 * @return the extracted distribution
	 */
	static Distro fromImage(Image image) {
		Assert.notNull(image, "'image' must not be null");
		Map<String, String> labels = image.getConfig().getLabels();
		String name = getLabel(labels, NAME_LABEL, STACK_NAME_LABEL);
		String version = getLabel(labels, VERSION_LABEL, STACK_VERSION_LABEL);
		return new Distro(name, version);
	}

	private static @Nullable String getLabel(Map<String, String> labels, String label, String fallbackLabel) {
		String value = labels.get(label);
		if (StringUtils.hasText(value)) {
			return value;
		}
		value = labels.get(fallbackLabel);
		return StringUtils.hasText(value) ? value : null;
	}

}
