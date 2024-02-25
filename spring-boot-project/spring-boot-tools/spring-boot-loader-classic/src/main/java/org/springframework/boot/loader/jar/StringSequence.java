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

package org.springframework.boot.loader.jar;

import java.util.Objects;

/**
 * A {@link CharSequence} backed by a single shared {@link String}. Unlike a regular
 * {@link String}, {@link #subSequence(int, int)} operations will not copy the underlying
 * character array.
 *
 * @author Phillip Webb
 */
final class StringSequence implements CharSequence {

	private final String source;

	private final int start;

	private final int end;

	private int hash;

	/**
	 * Constructs a new StringSequence object with the given source string.
	 * @param source the source string to be used for the StringSequence object
	 */
	StringSequence(String source) {
		this(source, 0, (source != null) ? source.length() : -1);
	}

	/**
	 * Constructs a new StringSequence object with the specified source, start, and end
	 * values.
	 * @param source the source string to create the sequence from (must not be null)
	 * @param start the starting index of the sequence (must be non-negative)
	 * @param end the ending index of the sequence (must be within the bounds of the
	 * source string)
	 * @throws NullPointerException if the source string is null
	 * @throws StringIndexOutOfBoundsException if the start or end values are out of
	 * bounds
	 */
	StringSequence(String source, int start, int end) {
		Objects.requireNonNull(source, "Source must not be null");
		if (start < 0) {
			throw new StringIndexOutOfBoundsException(start);
		}
		if (end > source.length()) {
			throw new StringIndexOutOfBoundsException(end);
		}
		this.source = source;
		this.start = start;
		this.end = end;
	}

	/**
	 * Returns a new StringSequence that is a subsequence of this StringSequence. The
	 * subsequence starts at the specified index and extends to the end of this
	 * StringSequence.
	 * @param start the starting index of the subsequence
	 * @return a new StringSequence that is a subsequence of this StringSequence
	 * @throws IndexOutOfBoundsException if the start index is out of range
	 */
	StringSequence subSequence(int start) {
		return subSequence(start, length());
	}

	/**
	 * Returns a new StringSequence that is a subsequence of this StringSequence. The
	 * subsequence starts at the specified start index and extends to the character at
	 * index end - 1.
	 * @param start the start index of the subsequence (inclusive)
	 * @param end the end index of the subsequence (exclusive)
	 * @return a new StringSequence that is a subsequence of this StringSequence
	 * @throws StringIndexOutOfBoundsException if the start or end index is out of range
	 */
	@Override
	public StringSequence subSequence(int start, int end) {
		int subSequenceStart = this.start + start;
		int subSequenceEnd = this.start + end;
		if (subSequenceStart > this.end) {
			throw new StringIndexOutOfBoundsException(start);
		}
		if (subSequenceEnd > this.end) {
			throw new StringIndexOutOfBoundsException(end);
		}
		if (start == 0 && subSequenceEnd == this.end) {
			return this;
		}
		return new StringSequence(this.source, subSequenceStart, subSequenceEnd);
	}

	/**
	 * Returns {@code true} if the sequence is empty. Public to be compatible with JDK 15.
	 * @return {@code true} if {@link #length()} is {@code 0}, otherwise {@code false}
	 */
	public boolean isEmpty() {
		return length() == 0;
	}

	/**
	 * Returns the length of the StringSequence object.
	 * @return the length of the StringSequence object
	 */
	@Override
	public int length() {
		return this.end - this.start;
	}

	/**
	 * Returns the character at the specified index in the StringSequence.
	 * @param index the index of the character to be returned
	 * @return the character at the specified index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	@Override
	public char charAt(int index) {
		return this.source.charAt(this.start + index);
	}

	/**
	 * Returns the index within this StringSequence of the first occurrence of the
	 * specified character, starting the search at the specified index.
	 * @param ch the character to search for
	 * @return the index of the first occurrence of the character, relative to the start
	 * index
	 */
	int indexOf(char ch) {
		return this.source.indexOf(ch, this.start) - this.start;
	}

	/**
	 * Returns the index within this StringSequence of the first occurrence of the
	 * specified string. The search for the string starts at the specified start position.
	 * @param str the string to search for
	 * @return the index of the first occurrence of the specified string, or -1 if the
	 * string is not found
	 */
	int indexOf(String str) {
		return this.source.indexOf(str, this.start) - this.start;
	}

	/**
	 * Returns the index within this StringSequence of the first occurrence of the
	 * specified string, starting the search at the specified index.
	 * @param str the string to search for
	 * @param fromIndex the index to start the search from
	 * @return the index of the first occurrence of the specified string within this
	 * StringSequence, starting the search at the specified index; or -1 if the string is
	 * not found
	 */
	int indexOf(String str, int fromIndex) {
		return this.source.indexOf(str, this.start + fromIndex) - this.start;
	}

	/**
	 * Returns true if the string sequence starts with the specified prefix.
	 * @param prefix the prefix to check
	 * @return true if the string sequence starts with the prefix, false otherwise
	 */
	boolean startsWith(String prefix) {
		return startsWith(prefix, 0);
	}

	/**
	 * Checks if the specified prefix occurs at the given offset in the string sequence.
	 * @param prefix the prefix to be checked
	 * @param offset the offset at which to start checking
	 * @return {@code true} if the string sequence starts with the specified prefix at the
	 * given offset, {@code false} otherwise
	 */
	boolean startsWith(String prefix, int offset) {
		int prefixLength = prefix.length();
		int length = length();
		if (length - prefixLength - offset < 0) {
			return false;
		}
		return this.source.startsWith(prefix, this.start + offset);
	}

	/**
	 * Compares this StringSequence object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the specified object is equal to this StringSequence object, false
	 * otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CharSequence other)) {
			return false;
		}
		int n = length();
		if (n != other.length()) {
			return false;
		}
		int i = 0;
		while (n-- != 0) {
			if (charAt(i) != other.charAt(i)) {
				return false;
			}
			i++;
		}
		return true;
	}

	/**
	 * Returns the hash code value for this StringSequence object.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		int hash = this.hash;
		if (hash == 0 && length() > 0) {
			for (int i = this.start; i < this.end; i++) {
				hash = 31 * hash + this.source.charAt(i);
			}
			this.hash = hash;
		}
		return hash;
	}

	/**
	 * Returns a string representation of the substring of the source string specified by
	 * the start and end indices.
	 * @return the substring of the source string
	 */
	@Override
	public String toString() {
		return this.source.substring(this.start, this.end);
	}

}
