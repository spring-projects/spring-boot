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

package org.springframework.boot.testcontainers.service.connection;

import java.lang.reflect.Field;

import org.springframework.boot.origin.Origin;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link Origin} backed by a {@link Field}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class FieldOrigin implements Origin {

	private final Field field;

	FieldOrigin(Field field) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		FieldOrigin other = (FieldOrigin) obj;
		return this.field.equals(other.field);
	}

	@Override
	public int hashCode() {
		return this.field.hashCode();
	}

	@Override
	public String toString() {
		return ClassUtils.getShortName(this.field.getDeclaringClass()) + "." + this.field.getName();
	}

}
