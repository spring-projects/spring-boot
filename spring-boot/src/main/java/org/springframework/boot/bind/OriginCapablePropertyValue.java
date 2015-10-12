/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.bind;

import org.springframework.beans.PropertyValue;
import org.springframework.core.env.PropertySource;

/**
 * A {@link PropertyValue} that can provide information about its origin.
 *
 * @author Andy Wilkinson
 */
class OriginCapablePropertyValue extends PropertyValue {

	private static final String ATTRIBUTE_PROPERTY_ORIGIN = "propertyOrigin";

	private final PropertyOrigin origin;

	OriginCapablePropertyValue(PropertyValue propertyValue) {
		this(propertyValue.getName(), propertyValue.getValue(),
				(PropertyOrigin) propertyValue.getAttribute(ATTRIBUTE_PROPERTY_ORIGIN));
	}

	OriginCapablePropertyValue(String name, Object value, String originName,
			PropertySource<?> originSource) {
		this(name, value, new PropertyOrigin(originSource, originName));
	}

	OriginCapablePropertyValue(String name, Object value, PropertyOrigin origin) {
		super(name, value);
		this.origin = origin;
		setAttribute(ATTRIBUTE_PROPERTY_ORIGIN, origin);
	}

	public PropertyOrigin getOrigin() {
		return this.origin;
	}

	@Override
	public String toString() {
		String name = this.origin != null ? this.origin.getName() : this.getName();
		String source = this.origin.getSource() != null
				? this.origin.getSource().getName() : "unknown";
		return "'" + name + "' from '" + source + "'";
	}

	public static PropertyOrigin getOrigin(PropertyValue propertyValue) {
		if (propertyValue instanceof OriginCapablePropertyValue) {
			return ((OriginCapablePropertyValue) propertyValue).getOrigin();
		}
		return new OriginCapablePropertyValue(propertyValue).getOrigin();
	}

}
