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

package org.springframework.boot.loader.zip;

import java.util.BitSet;

/**
 * Tracks entries that have a name that should be offset by a specific amount. This class
 * is used with nested directory zip files so that entries under the directory are offset
 * correctly. META-INF entries are copied directly and have no offset.
 *
 * @author Phillip Webb
 */
class NameOffsetLookups {

	public static final NameOffsetLookups NONE = new NameOffsetLookups(0, 0);

	private final int offset;

	private final BitSet enabled;

	/**
	 * Constructs a new NameOffsetLookups object with the specified offset and size.
	 * @param offset the offset value to set
	 * @param size the size value to set
	 */
	NameOffsetLookups(int offset, int size) {
		this.offset = offset;
		this.enabled = (size != 0) ? new BitSet(size) : null;
	}

	/**
	 * Swaps the values at the specified indices in the 'enabled' list.
	 * @param i the index of the first element to be swapped
	 * @param j the index of the second element to be swapped
	 */
	void swap(int i, int j) {
		if (this.enabled != null) {
			boolean temp = this.enabled.get(i);
			this.enabled.set(i, this.enabled.get(j));
			this.enabled.set(j, temp);
		}
	}

	/**
	 * Retrieves the offset value for the specified index.
	 * @param index the index for which the offset value is to be retrieved
	 * @return the offset value if the index is enabled, otherwise 0
	 */
	int get(int index) {
		return isEnabled(index) ? this.offset : 0;
	}

	/**
	 * Enables or disables the specified index in the NameOffsetLookups object.
	 * @param index The index to enable or disable.
	 * @param enable True to enable the index, false to disable it.
	 * @return The offset value if enable is true, otherwise 0.
	 */
	int enable(int index, boolean enable) {
		if (this.enabled != null) {
			this.enabled.set(index, enable);
		}
		return (!enable) ? 0 : this.offset;
	}

	/**
	 * Returns a boolean value indicating whether the element at the specified index is
	 * enabled.
	 * @param index the index of the element to check
	 * @return {@code true} if the element is enabled, {@code false} otherwise
	 */
	boolean isEnabled(int index) {
		return (this.enabled != null && this.enabled.get(index));
	}

	/**
	 * Checks if any of the elements in the enabled BitSet are set to true.
	 * @return true if at least one element in the enabled BitSet is set to true, false
	 * otherwise.
	 */
	boolean hasAnyEnabled() {
		return this.enabled != null && this.enabled.cardinality() > 0;
	}

	/**
	 * Creates and returns a new instance of the NameOffsetLookups class with the same
	 * offset and an empty enabled list.
	 * @return A new instance of the NameOffsetLookups class.
	 */
	NameOffsetLookups emptyCopy() {
		return new NameOffsetLookups(this.offset, this.enabled.size());
	}

}
