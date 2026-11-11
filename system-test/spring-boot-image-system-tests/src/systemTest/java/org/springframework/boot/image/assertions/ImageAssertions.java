/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.image.assertions;

import java.io.IOException;

import com.github.dockerjava.api.model.ContainerConfig;

import org.springframework.boot.buildpack.platform.docker.type.ImageReference;

/**
 * Factory class for custom AssertJ {@link org.assertj.core.api.Assert}s related to images
 * and containers.
 *
 * @author Scott Frederick
 */
public final class ImageAssertions {

	private ImageAssertions() {
	}

	public static ContainerConfigAssert assertThat(ContainerConfig containerConfig) {
		return new ContainerConfigAssert(containerConfig);
	}

	public static ImageAssert assertThat(ImageReference imageReference) throws IOException {
		return new ImageAssert(imageReference);
	}

}
