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

import java.util.Objects;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Details of a cache for use by the CNB builder.
 *
 * @author Scott Frederick
 * @since 2.6.0
 */
public class Cache {

	/**
	 * The format of the cache.
	 */
	public enum Format {

		/**
		 * A cache stored as a volume in the Docker daemon.
		 */
		VOLUME;

	}

	protected final Format format;

	Cache(Format format) {
		this.format = format;
	}

	/**
	 * Return the details of the cache if it is a volume cache.
	 * @return the cache, or {@code null} if it is not a volume cache
	 */
	public Volume getVolume() {
		return (this.format.equals(Format.VOLUME)) ? (Volume) this : null;
	}

	/**
	 * Create a new {@code Cache} that uses a volume with the provided name.
	 * @param name the cache volume name
	 * @return a new cache instance
	 */
	public static Cache volume(String name) {
		Assert.notNull(name, "Name must not be null");
		return new Volume(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Cache other = (Cache) obj;
		return Objects.equals(this.format, other.format);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.format);
	}

	/**
	 * Details of a cache stored in a Docker volume.
	 */
	public static class Volume extends Cache {

		private final String name;

		Volume(String name) {
			super(Format.VOLUME);
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			if (!super.equals(obj)) {
				return false;
			}
			Volume other = (Volume) obj;
			return Objects.equals(this.name, other.name);
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + ObjectUtils.nullSafeHashCode(this.name);
			return result;
		}

	}

}
