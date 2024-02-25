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

	/**
	 * Initializes a new instance of the FieldOrigin class with the specified field.
	 * @param field the field to set as the origin
	 * @throws IllegalArgumentException if the field is null
	 */
	FieldOrigin(Field field) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
	}

	/**
	 * Compares this FieldOrigin object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the specified object is equal to this FieldOrigin object, false
	 * otherwise
	 */
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

	/**
	 * Returns a hash code value for the object. The hash code is generated based on the
	 * hash code of the field.
	 * @return the hash code value for the object
	 */
	@Override
	public int hashCode() {
		return this.field.hashCode();
	}

	/**
	 * Returns a string representation of the current object. The string representation is
	 * in the format of the declaring class name followed by a dot and the field name.
	 * @return a string representation of the current object
	 */
	@Override
	public String toString() {
		return ClassUtils.getShortName(this.field.getDeclaringClass()) + "." + this.field.getName();
	}

}
