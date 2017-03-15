/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.env;

/**
 * Wrapper class for an Object {@code value} and {@link PropertyOrigin origin}.
 *
 * @author Madhura Bhave
 * @see OriginTrackedMapPropertySource
 */
class OriginTrackedValue {

	private final Object value;

	private final PropertyOrigin origin;

	OriginTrackedValue(Object value, PropertyOrigin origin) {
		this.value = value;
		this.origin = origin;
	}

	public Object getValue() {
		return this.value;
	}

	public PropertyOrigin getOrigin() {
		return this.origin;
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		return this.value.equals(((OriginTrackedValue) obj).value);
	}

	/**
	 * Create an {@link OriginTrackedValue} containing the specified {@code value} and
	 * {@code origin}. If the source value implements {@link CharSequence} then so will
	 * the resulting {@link OriginTrackedValue}.
	 * @param value the source value
	 * @param origin the origin
	 * @return a {@link OriginTrackedValue} or {@code null} if the source value was
	 * {@code null}.
	 */
	public static OriginTrackedValue of(Object value, PropertyOrigin origin) {
		if (value == null) {
			return null;
		}
		if (value instanceof CharSequence) {
			return new OriginTrackedCharSequence((CharSequence) value, origin);
		}
		return new OriginTrackedValue(value, origin);
	}

	/**
	 * {@link OriginTrackedValue} for a {@link CharSequence}.
	 */
	private static class OriginTrackedCharSequence extends OriginTrackedValue
			implements CharSequence {

		OriginTrackedCharSequence(CharSequence value, PropertyOrigin origin) {
			super(value, origin);
		}

		@Override
		public int length() {
			return getValue().length();
		}

		@Override
		public char charAt(int index) {
			return getValue().charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return getValue().subSequence(start, end);
		}

		@Override
		public CharSequence getValue() {
			return (CharSequence) super.getValue();
		}

	}

}
