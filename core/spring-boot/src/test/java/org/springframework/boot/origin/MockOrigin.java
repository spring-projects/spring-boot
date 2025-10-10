/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;
import org.springframework.util.Assert;

/**
 * Mock {@link Origin} implementation used for testing.
 *
 * @author Phillip Webb
 */
public final class MockOrigin implements Origin {

	private final String value;

	private final @Nullable Origin parent;

	private MockOrigin(String value, @Nullable Origin parent) {
		Assert.notNull(value, "'value' must not be null");
		this.value = value;
		this.parent = parent;
	}

	@Override
	public @Nullable Origin getParent() {
		return this.parent;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.value.equals(((MockOrigin) obj).value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	@Contract("!null -> !null")
	public static @Nullable Origin of(@Nullable String value) {
		return of(value, null);
	}

	@Contract("!null, _ -> !null")
	public static @Nullable Origin of(@Nullable String value, @Nullable Origin parent) {
		return (value != null) ? new MockOrigin(value, parent) : null;
	}

}
