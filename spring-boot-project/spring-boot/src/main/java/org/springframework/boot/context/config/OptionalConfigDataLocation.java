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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ConfigDataLocation} wrapper used to indicate that it's optional.
 *
 * @author Phillip Webb
 */
class OptionalConfigDataLocation extends ConfigDataLocation {

	private ConfigDataLocation location;

	OptionalConfigDataLocation(ConfigDataLocation location) {
		this.location = location;
	}

	ConfigDataLocation getLocation() {
		return this.location;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		OptionalConfigDataLocation other = (OptionalConfigDataLocation) obj;
		return this.location.equals(other.location);
	}

	@Override
	public int hashCode() {
		return this.location.hashCode();
	}

	@Override
	public String toString() {
		return this.location.toString();
	}

	static List<ConfigDataLocation> wrapAll(List<ConfigDataLocation> locations) {
		List<ConfigDataLocation> wrapped = new ArrayList<>(locations.size());
		locations.forEach((location) -> wrapped.add(new OptionalConfigDataLocation(location)));
		return wrapped;
	}

	@SuppressWarnings("unchecked")
	static <L extends ConfigDataLocation> L unwrap(ConfigDataLocation wrapped) {
		return (L) ((OptionalConfigDataLocation) wrapped).getLocation();
	}

}
