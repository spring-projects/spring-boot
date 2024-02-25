/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.util.ObjectUtils;

/**
 * A wrapper for an {@link Object} value and {@link Origin}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 * @see #of(Object)
 * @see #of(Object, Origin)
 */
public class OriginTrackedValue implements OriginProvider {

	private final Object value;

	private final Origin origin;

	/**
	 * Constructs a new OriginTrackedValue with the specified value and origin.
	 * @param value the value to be stored in the OriginTrackedValue
	 * @param origin the origin of the value
	 */
	private OriginTrackedValue(Object value, Origin origin) {
		this.value = value;
		this.origin = origin;
	}

	/**
	 * Return the tracked value.
	 * @return the tracked value
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Returns the origin of the OriginTrackedValue.
	 * @return the origin of the OriginTrackedValue
	 */
	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	/**
	 * Compares this OriginTrackedValue with the specified object for equality.
	 * @param obj the object to compare with
	 * @return true if the specified object is equal to this OriginTrackedValue, false
	 * otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(this.value, ((OriginTrackedValue) obj).value);
	}

	/**
	 * Returns a hash code value for the object. This method overrides the default
	 * implementation of the {@code hashCode} method inherited from the {@code Object}
	 * class.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value);
	}

	/**
	 * Returns a string representation of the value.
	 * @return the string representation of the value, or null if the value is null
	 */
	@Override
	public String toString() {
		return (this.value != null) ? this.value.toString() : null;
	}

	/**
	 * Creates a new OriginTrackedValue object with the specified value and no origin.
	 * @param value the value to be stored in the OriginTrackedValue object
	 * @return a new OriginTrackedValue object with the specified value and no origin
	 */
	public static OriginTrackedValue of(Object value) {
		return of(value, null);
	}

	/**
	 * Create an {@link OriginTrackedValue} containing the specified {@code value} and
	 * {@code origin}. If the source value implements {@link CharSequence} then so will
	 * the resulting {@link OriginTrackedValue}.
	 * @param value the source value
	 * @param origin the origin
	 * @return an {@link OriginTrackedValue} or {@code null} if the source value was
	 * {@code null}.
	 */
	public static OriginTrackedValue of(Object value, Origin origin) {
		if (value == null) {
			return null;
		}
		if (value instanceof CharSequence charSequence) {
			return new OriginTrackedCharSequence(charSequence, origin);
		}
		return new OriginTrackedValue(value, origin);
	}

	/**
	 * {@link OriginTrackedValue} for a {@link CharSequence}.
	 */
	private static class OriginTrackedCharSequence extends OriginTrackedValue implements CharSequence {

		/**
		 * Constructs a new OriginTrackedCharSequence with the specified value and origin.
		 * @param value the underlying character sequence
		 * @param origin the origin of the character sequence
		 */
		OriginTrackedCharSequence(CharSequence value, Origin origin) {
			super(value, origin);
		}

		/**
		 * Returns the length of the value stored in the OriginTrackedCharSequence object.
		 * @return the length of the value
		 */
		@Override
		public int length() {
			return getValue().length();
		}

		/**
		 * Returns the character at the specified index in the sequence.
		 * @param index the index of the character to be returned
		 * @return the character at the specified index
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 ||
		 * index >= length())
		 */
		@Override
		public char charAt(int index) {
			return getValue().charAt(index);
		}

		/**
		 * Returns a new CharSequence that is a subsequence of this sequence.
		 * @param start the start index, inclusive.
		 * @param end the end index, exclusive.
		 * @return the specified subsequence.
		 * @throws IndexOutOfBoundsException if {@code start} or {@code end} are negative,
		 * if {@code end} is greater than {@code start}, or if {@code end} is greater than
		 * the length of this sequence.
		 */
		@Override
		public CharSequence subSequence(int start, int end) {
			return getValue().subSequence(start, end);
		}

		/**
		 * Returns the value of the OriginTrackedCharSequence object.
		 * @return the value of the OriginTrackedCharSequence object as a CharSequence
		 */
		@Override
		public CharSequence getValue() {
			return (CharSequence) super.getValue();
		}

	}

}
