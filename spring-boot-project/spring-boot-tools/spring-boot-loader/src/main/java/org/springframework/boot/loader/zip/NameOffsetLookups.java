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

	NameOffsetLookups(int offset, int size) {
		this.offset = offset;
		this.enabled = (size != 0) ? new BitSet(size) : null;
	}

	void swap(int i, int j) {
		if (this.enabled != null) {
			boolean temp = this.enabled.get(i);
			this.enabled.set(i, this.enabled.get(j));
			this.enabled.set(j, temp);
		}
	}

	int get(int index) {
		return isEnabled(index) ? this.offset : 0;
	}

	int enable(int index, boolean enable) {
		if (this.enabled != null) {
			this.enabled.set(index, enable);
		}
		return (!enable) ? 0 : this.offset;
	}

	boolean isEnabled(int index) {
		return (this.enabled != null && this.enabled.get(index));
	}

	boolean hasAnyEnabled() {
		return this.enabled != null && this.enabled.cardinality() > 0;
	}

	NameOffsetLookups emptyCopy() {
		return new NameOffsetLookups(this.offset, this.enabled.size());
	}

}
