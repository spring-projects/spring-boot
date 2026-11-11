/*
 * Copyright 2012-present the original author or authors.
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
		Assert.notNull(field, "'field' must not be null");
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
