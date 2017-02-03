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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple mock implementation of {@link CanonicalPropertySource}.
 *
 * @author Phillip Webb
 */
public class MockCanonicalPropertySource implements CanonicalPropertySource {

	private final Map<CanonicalPropertyName, CanonicalPropertyValue> map;

	public MockCanonicalPropertySource() {
		this.map = new LinkedHashMap<>();
	}

	public MockCanonicalPropertySource(String name, String value) {
		this(name, value, null);
	}

	public MockCanonicalPropertySource(String name, String value, String origin) {
		this.map = new LinkedHashMap<>();
		add(name, value, origin);
	}

	@Override
	public CanonicalPropertyValue getProperty(CanonicalPropertyName name) {
		return this.map.get(name);
	}

	@Override
	public Set<CanonicalPropertyName> getCanonicalPropertyNames() {
		return this.map.keySet();
	}

	public void add(String name, Object value) {
		add(name, value, null);
	}

	public void add(String name, Object value, String origin) {
		add(new CanonicalPropertyName(name), value, origin);
	}

	public void add(CanonicalPropertyName name, Object value, String origin) {
		add(name, new MockCanonicalPropertyValue(value, MockPropertyOrigin.get(origin)));
	}

	public void add(CanonicalPropertyName name, CanonicalPropertyValue value) {
		this.map.put(name, value);
	}

	/**
	 * Simple mock implementation of {@link CanonicalPropertyValue}.
	 */
	private static class MockCanonicalPropertyValue implements CanonicalPropertyValue {

		private final Object value;

		private final PropertyOrigin origin;

		public MockCanonicalPropertyValue(Object value, PropertyOrigin origin) {
			this.value = value;
			this.origin = origin;
		}

		@Override
		public Object getValue() {
			return this.value;
		}

		@Override
		public PropertyOrigin getOrigin() {
			return this.origin;
		}

	}

	/**
	 * Simple mock implementation of {@link PropertyOrigin}.
	 */
	private static class MockPropertyOrigin implements PropertyOrigin {

		private final String origin;

		public MockPropertyOrigin(String origin) {
			this.origin = origin;
		}

		@Override
		public String toString() {
			return this.origin;
		}

		public static PropertyOrigin get(String origin) {
			return origin == null ? null : new MockPropertyOrigin(origin);
		}

	}

}
