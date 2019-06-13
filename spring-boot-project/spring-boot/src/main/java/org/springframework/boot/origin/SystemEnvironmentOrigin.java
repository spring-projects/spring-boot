/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.origin;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link Origin} for an item loaded from the system environment. Provides access to the
 * original property name.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class SystemEnvironmentOrigin implements Origin {

	private final String property;

	public SystemEnvironmentOrigin(String property) {
		Assert.notNull(property, "Property name must not be null");
		Assert.hasText(property, "Property name must not be empty");
		this.property = property;
	}

	public String getProperty() {
		return this.property;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SystemEnvironmentOrigin other = (SystemEnvironmentOrigin) obj;
		return ObjectUtils.nullSafeEquals(this.property, other.property);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.property);
	}

	@Override
	public String toString() {
		return "System Environment Property \"" + this.property + "\"";
	}

}
