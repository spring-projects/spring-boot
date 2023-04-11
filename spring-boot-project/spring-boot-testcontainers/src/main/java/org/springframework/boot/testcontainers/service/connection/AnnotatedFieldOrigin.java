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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;

import org.springframework.boot.origin.Origin;
import org.springframework.util.Assert;

/**
 * {@link Origin} backed by a {@link Field} and an {@link Annotation}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class AnnotatedFieldOrigin implements Origin {

	private final Field field;

	private final Annotation annotation;

	AnnotatedFieldOrigin(Field field, Annotation annotation) {
		Assert.notNull(field, "Field must not be null");
		Assert.notNull(annotation, "Annotation must not be null");
		this.field = field;
		this.annotation = annotation;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		AnnotatedFieldOrigin other = (AnnotatedFieldOrigin) obj;
		return this.field.equals(other.field) && this.annotation.equals(other.annotation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.field, this.annotation);
	}

	@Override
	public String toString() {
		return this.annotation + " " + this.field;
	}

}
