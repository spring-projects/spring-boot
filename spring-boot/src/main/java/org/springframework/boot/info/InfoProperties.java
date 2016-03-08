/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.info;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * Base class for components exposing unstructured data with dedicated methods for
 * well known keys.
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
		Assert.notNull(entries, "Properties must not be null");
		this.entries = copy(entries);
	}

	/**
	 * Return the value of the specified property or {@code null}.
	 * @param property the id of the property
	 * @return the property value
	 */
	public String get(String property) {
		return this.entries.getProperty(property);
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

	private static Properties copy(Properties properties) {
		Properties copy = new Properties();
		copy.putAll(properties);
		return copy;
	}

	/**
	 * Property entry.
	 */
	public final class Entry {

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

	private final class PropertiesIterator implements Iterator<Entry> {

		private final Properties properties;

		private final Enumeration<Object> keys;

		private PropertiesIterator(Properties properties) {
			this.properties = properties;
			this.keys = this.properties.keys();
		}

		@Override
		public boolean hasNext() {
			return this.keys.hasMoreElements();
		}

		@Override
		public Entry next() {
			String key = (String) this.keys.nextElement();
			return new Entry(key, this.properties.getProperty(key));
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("InfoProperties are immutable.");
		}

	}

}
