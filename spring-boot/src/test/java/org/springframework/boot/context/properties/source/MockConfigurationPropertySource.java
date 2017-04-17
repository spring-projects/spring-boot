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

package org.springframework.boot.context.properties.source;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.origin.MockOrigin;
import org.springframework.boot.origin.OriginTrackedValue;

/**
 * Mock {@link ConfigurationPropertySource} implementation used for testing.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class MockConfigurationPropertySource implements ConfigurationPropertySource {

	private final Map<ConfigurationPropertyName, OriginTrackedValue> map = new LinkedHashMap<>();

	private boolean nonIterable;

	public MockConfigurationPropertySource() {
	}

	public MockConfigurationPropertySource(String configurationPropertyName,
			Object value) {
		this(configurationPropertyName, value, null);
	}

	public MockConfigurationPropertySource(String configurationPropertyName, Object value,
			String origin) {
		put(ConfigurationPropertyName.of(configurationPropertyName),
				OriginTrackedValue.of(value, MockOrigin.of(origin)));
	}

	public void put(String name, String value) {
		put(ConfigurationPropertyName.of(name), value);
	}

	public void put(ConfigurationPropertyName name, String value) {
		put(name, OriginTrackedValue.of(value));
	}

	private void put(ConfigurationPropertyName name, OriginTrackedValue value) {
		this.map.put(name, value);
	}

	public void setNonIterable(boolean nonIterable) {
		this.nonIterable = nonIterable;
	}

	@Override
	public Iterator<ConfigurationPropertyName> iterator() {
		if (this.nonIterable) {
			return Collections.<ConfigurationPropertyName>emptyList().iterator();
		}
		return this.map.keySet().iterator();
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		if (this.nonIterable) {
			return Collections.<ConfigurationPropertyName>emptyList().stream();
		}
		return this.map.keySet().stream();
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(
			ConfigurationPropertyName name) {
		OriginTrackedValue result = this.map.get(name);
		if (result == null) {
			result = findValue(name);
		}
		return ConfigurationProperty.of(name, result);
	}

	private OriginTrackedValue findValue(ConfigurationPropertyName name) {
		return this.map.get(name);
	}

}
