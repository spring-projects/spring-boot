/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

/**
 * Helper for properties-related operations.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class PropertyHelper {

	private final PropertyResolver propertyResolver;

	private final String prefix;

	private final boolean relaxedNames;

	/**
	 * Create a new instance with the base {@link PropertyResolver} to use. If
	 * a prefix is set and does not end with a dot, it is added.
	 *
	 * @param propertyResolver the base property resolver
	 * @param prefix the prefix to lookup keys, if any
	 * @param relaxedNames if relaxed names should be checked
	 */
	PropertyHelper(PropertyResolver propertyResolver, String prefix, boolean relaxedNames) {
		this.prefix = cleanPrefix(prefix);
		this.relaxedNames = relaxedNames;
		if (relaxedNames) {
			this.propertyResolver = new RelaxedPropertyResolver(propertyResolver, this.prefix);
		}
		else {
			this.propertyResolver = propertyResolver;
		}
	}

	/**
	 * Return the {@link PropertyResolver} to use.
	 */
	public PropertyResolver getPropertyResolver() {
		return propertyResolver;
	}

	/**
	 * Create the full property key for the specified property. Applies the
	 * configured prefix, if necessary.
	 */
	public String createPropertyKey(String property) {
		return (relaxedNames ? property : prefix + property);
	}

	private static String cleanPrefix(String prefix) {
		return (StringUtils.hasText(prefix) && !prefix.endsWith(".") ? prefix + "." : prefix);
	}
}
