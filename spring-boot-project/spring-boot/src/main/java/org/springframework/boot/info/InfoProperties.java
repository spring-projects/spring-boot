/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.info;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * Base class for components exposing unstructured data with dedicated methods for well
 * known keys.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class InfoProperties implements Iterable<InfoProperties.Entry> {

	private final Properties entries;

	/**
	 * Create an instance with the specified entries.
	 * @param entries the information to expose
	 */
	public InfoProperties(Properties entries) {
		Assert.notNull(entries, "Entries must not be null");
		this.entries = copy(entries);
	}

	/**
	 * Return the value of the specified property or {@code null}.
	 * @param key the key of the property
	 * @return the property value
	 */
	public String get(String key) {
		return this.entries.getProperty(key);
	}

	/**
	 * Return the value of the specified property as an {@link Instant} or {@code null} if
	 * the value is not a valid {@link Long} representation of an epoch time.
	 * @param key the key of the property
	 * @return the property value
	 */
	public Instant getInstant(String key) {
		String s = get(key);
		if (s != null) {
			try {
				return Instant.ofEpochMilli(Long.parseLong(s));
			}
			catch (NumberFormatException ex) {
				// Not valid epoch time
			}
		}
		return null;
	}

	@Override
	public Iterator<Entry> iterator() {
		return new PropertiesIterator(this.entries);
	}

	/**
	 * Return a {@link PropertySource} of this instance.
	 * @return a {@link PropertySource}
	 */
	public PropertySource<?> toPropertySource() {
		return new PropertiesPropertySource(getClass().getSimpleName(), copy(this.entries));
	}

	private Properties copy(Properties properties) {
		Properties copy = new Properties();
		copy.putAll(properties);
		return copy;
	}

	private final class PropertiesIterator implements Iterator<Entry> {

		private final Iterator<Map.Entry<Object, Object>> iterator;

		private PropertiesIterator(Properties properties) {
			this.iterator = properties.entrySet().iterator();
		}

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public Entry next() {
			Map.Entry<Object, Object> entry = this.iterator.next();
			return new Entry((String) entry.getKey(), (String) entry.getValue());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("InfoProperties are immutable.");
		}

	}

	/**
	 * Property entry.
	 */
	public static final class Entry {

		private final String key;

		private final String value;

		private Entry(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return this.key;
		}

		public String getValue() {
			return this.value;
		}

	}

}
