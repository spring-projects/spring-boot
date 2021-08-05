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
