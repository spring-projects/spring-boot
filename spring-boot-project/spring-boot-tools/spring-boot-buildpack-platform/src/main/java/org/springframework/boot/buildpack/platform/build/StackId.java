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

package org.springframework.boot.buildpack.platform.build;

import java.util.Map;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A Stack ID.
 *
 * @author Phillip Webb
 */
class StackId {

	private static final String LABEL_NAME = "io.buildpacks.stack.id";

	private final String value;

	StackId(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.value.equals(((StackId) obj).value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Factory method to create a {@link StackId} from an {@link Image}.
	 * @param image the source image
	 * @return the extracted stack ID
	 */
	static StackId fromImage(Image image) {
		Assert.notNull(image, "Image must not be null");
		return fromImageConfig(image.getConfig());
	}

	/**
	 * Factory method to create a {@link StackId} from an {@link ImageConfig}.
	 * @param imageConfig the source image config
	 * @return the extracted stack ID
	 */
	private static StackId fromImageConfig(ImageConfig imageConfig) {
		Map<String, String> labels = imageConfig.getLabels();
		String value = (labels != null) ? labels.get(LABEL_NAME) : null;
		Assert.state(StringUtils.hasText(value), () -> "Missing '" + LABEL_NAME + "' stack label");
		return new StackId(value);
	}

	/**
	 * Factory method to create a {@link StackId} with a given value.
	 * @param value the stack ID value
	 * @return a new stack ID instance
	 */
	static StackId of(String value) {
		Assert.hasText(value, "Value must not be empty");
		return new StackId(value);
	}

}
