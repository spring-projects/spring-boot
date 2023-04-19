/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.core;

/**
 * A docker image reference of form
 * {@code [<registry>/][<project>/]<image>[:<tag>|@<digest>]}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see <a href="https://docs.docker.com/compose/compose-file/#image">docker
 * documentation</a>
 */
public final class ImageReference {

	private final String reference;

	private final String imageName;

	ImageReference(String reference) {
		this.reference = reference;
		int lastSlashIndex = reference.lastIndexOf('/');
		String imageTagDigest = (lastSlashIndex != -1) ? reference.substring(lastSlashIndex + 1) : reference;
		int digestIndex = imageTagDigest.indexOf('@');
		String imageTag = (digestIndex != -1) ? imageTagDigest.substring(0, digestIndex) : imageTagDigest;
		int colon = imageTag.indexOf(':');
		this.imageName = (colon != -1) ? imageTag.substring(0, colon) : imageTag;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ImageReference other = (ImageReference) obj;
		return this.reference.equals(other.reference);
	}

	@Override
	public int hashCode() {
		return this.reference.hashCode();
	}

	@Override
	public String toString() {
		return this.reference;
	}

	/**
	 * Return the referenced image, excluding the registry or project. For example, a
	 * reference of {@code my_private.registry:5000/redis:5} would return {@code redis}.
	 * @return the referenced image
	 */
	public String getImageName() {
		return this.imageName;
	}

	/**
	 * Create an image reference from the given String value.
	 * @param value the string used to create the reference
	 * @return an {@link ImageReference} instance
	 */
	public static ImageReference of(String value) {
		return (value != null) ? new ImageReference(value) : null;
	}

}
