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

import java.util.Map;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * {@link SystemEnvironmentPropertySource} and {@link OriginLookup} backed by a map
 * containing {@link OriginTrackedValue OriginTrackedValues}.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class OriginTrackedSystemPropertySource extends SystemEnvironmentPropertySource implements OriginLookup<String> {

	/**
	 * Create a new {@code OriginTrackedSystemPropertySource} with the given name and
	 * delegating to the given {@code MapPropertySource}.
	 * @param name the property name
	 * @param source the underlying map
	 */
	public OriginTrackedSystemPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}

	@Override
	public Object getProperty(String name) {
		Object value = super.getProperty(name);
		if (value instanceof OriginTrackedValue) {
			return ((OriginTrackedValue) value).getValue();
		}
		return value;
	}

	@Override
	public Origin getOrigin(String key) {
		Object value = super.getProperty(key);
		if (value instanceof OriginTrackedValue) {
			return ((OriginTrackedValue) value).getOrigin();
		}
		return null;
	}
}

