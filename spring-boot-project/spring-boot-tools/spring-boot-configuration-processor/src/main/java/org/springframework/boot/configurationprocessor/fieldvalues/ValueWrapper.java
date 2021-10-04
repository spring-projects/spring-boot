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

package org.springframework.boot.configurationprocessor.fieldvalues;

import java.util.Objects;

import javax.lang.model.element.TypeElement;

/**
 * The value assigned to a field in a {@link TypeElement}, including information related
 * to value resolution.
 *
 * @author Chris Bono
 * @since 2.6
 */
public class ValueWrapper {

	private final Object value;
	private final String valueInitializerExpression;
	private final boolean valueDetermined;

	private ValueWrapper(Object value, String valueInitializerExpression, boolean valueDetermined) {
		this.value = value;
		this.valueInitializerExpression = valueInitializerExpression;
		this.valueDetermined = valueDetermined;
	}

	public static ValueWrapper of(Object value, String valueInitializerExpression) {
		return new ValueWrapper(value, valueInitializerExpression, true);
	}

	public static ValueWrapper unresolvable(String valueInitializerExpression) {
		return new ValueWrapper(null, valueInitializerExpression, false);
	}

	/**
	 * Return the wrapped value.
	 *
	 * @return the wrapped value or {@code null} if the value was initialized with
	 * {@code null} or the initializer expression was {@link #valueDetermined() unable to be determined}.
	 */
	public Object value() {
		return value;
	}

	/**
	 * Return the expression used to initialize the value.
	 *
	 * @return the expression used to initialize the value or {@code null} if no initializer
	 * was used to determine the value
	 */
	public String valueInitializerExpression() {
		return valueInitializerExpression;
	}

	/**
	 * Return whether or not the value was able to be determined.
	 *
	 * @return {@code true} if the value was able to be determined, {@code false} otherwise.
	 */
	public boolean valueDetermined() {
		return valueDetermined;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ValueWrapper other = (ValueWrapper) o;
		return this.valueDetermined == other.valueDetermined &&
				Objects.deepEquals(this.value, other.value) &&
				Objects.equals(this.valueInitializerExpression, other.valueInitializerExpression);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, valueInitializerExpression, valueDetermined);
	}

	@Override
	public String toString() {
		return "ValueWrapper{value=" + value
				+ ", valueInitializerExpression=" + valueInitializerExpression
				+ ", valueDetermined=" + valueDetermined + "}";
	}
}
