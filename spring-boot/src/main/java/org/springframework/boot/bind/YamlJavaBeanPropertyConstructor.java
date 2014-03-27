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

package org.springframework.boot.bind;

import java.beans.IntrospectionException;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.NodeId;

/**
 * Extended version of snakeyaml's Constructor class to facilitate mapping custom YAML
 * keys to Javabean property names.
 * 
 * @author Luke Taylor
 */
public class YamlJavaBeanPropertyConstructor extends Constructor {

	private final Map<Class<?>, Map<String, Property>> properties = new HashMap<Class<?>, Map<String, Property>>();

	private final PropertyUtils propertyUtils = new PropertyUtils();

	public YamlJavaBeanPropertyConstructor(Class<?> theRoot) {
		super(theRoot);
		this.yamlClassConstructors.put(NodeId.mapping,
				new CustomPropertyConstructMapping());
	}

	public YamlJavaBeanPropertyConstructor(Class<?> theRoot,
			Map<Class<?>, Map<String, String>> propertyAliases) {
		this(theRoot);
		for (Class<?> key : propertyAliases.keySet()) {
			Map<String, String> map = propertyAliases.get(key);
			if (map != null) {
				for (String alias : map.keySet()) {
					addPropertyAlias(alias, key, map.get(alias));
				}
			}
		}
	}

	/**
	 * Adds an alias for a Javabean property name on a particular type. The values of YAML
	 * keys with the alias name will be mapped to the Javabean property.
	 * @param alias the alias to map
	 * @param type the type of property
	 * @param name the property name
	 */
	protected final void addPropertyAlias(String alias, Class<?> type, String name) {
		Map<String, Property> typeMap = this.properties.get(type);

		if (typeMap == null) {
			typeMap = new HashMap<String, Property>();
			this.properties.put(type, typeMap);
		}

		try {
			typeMap.put(alias, this.propertyUtils.getProperty(type, name));
		}
		catch (IntrospectionException ex) {
			throw new RuntimeException(ex);
		}
	}

	class CustomPropertyConstructMapping extends ConstructMapping {

		@Override
		protected Property getProperty(Class<?> type, String name)
				throws IntrospectionException {
			Map<String, Property> forType = YamlJavaBeanPropertyConstructor.this.properties
					.get(type);
			Property property = (forType == null ? null : forType.get(name));
			return (property == null ? super.getProperty(type, name) : property);
		}

	}
}
