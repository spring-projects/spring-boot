/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.util.ObjectUtils;

/**
 * Details of a local cache stored as a volume or bind mount.
 *
 * @author Scott Frederick
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public sealed interface LocalCache extends Cache permits LocalCache.Volume, LocalCache.Bind {

	/**
	 * Details of a cache stored in a Docker volume.
	 */
	final class Volume implements LocalCache {

		private final VolumeName name;

		Volume(VolumeName name) {
			this.name = name;
		}

		public String getName() {
			return this.name.toString();
		}

		public VolumeName getVolumeName() {
			return this.name;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			Volume other = (Volume) obj;
			return Objects.equals(this.name, other.name);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.name);
		}

		@Override
		public String toString() {
			return "volume '" + this.name + "'";
		}

	}

	/**
	 * Details of a cache stored in a bind mount.
	 */
	final class Bind implements LocalCache {

		private final String source;

		Bind(String source) {
			this.source = source;
		}

		public String getSource() {
			return this.source;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			Bind other = (Bind) obj;
			return Objects.equals(this.source, other.source);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.source);
		}

		@Override
		public String toString() {
			return "bind mount '" + this.source + "'";
		}

	}

}
