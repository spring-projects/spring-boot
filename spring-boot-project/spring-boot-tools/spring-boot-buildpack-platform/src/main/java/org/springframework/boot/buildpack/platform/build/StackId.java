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

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A Stack ID.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class StackId {

	private static final String LABEL_NAME = "io.buildpacks.stack.id";

	private final String value;

	/**
     * Constructs a new StackId object with the specified value.
     *
     * @param value the value to be assigned to the StackId object
     */
    StackId(String value) {
		this.value = value;
	}

	/**
     * Compares this StackId object to the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return true if the specified object is equal to this StackId object, false otherwise
     */
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

	/**
     * Returns the hash code value for this StackId object.
     * 
     * @return the hash code value for this StackId object
     */
    @Override
	public int hashCode() {
		return this.value.hashCode();
	}

	/**
     * Returns a string representation of the StackId object.
     * 
     * @return the string value of the StackId object
     */
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
		String value = imageConfig.getLabels().get(LABEL_NAME);
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
