/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.bind;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

/**
 * Binder implementation that allows caller to bind to maps and also allows property names
 * to match a bit loosely (if underscores or dashes are removed and replaced with camel
 * case for example).
 * 
 * @author Dave Syer
 */
public class RelaxedDataBinder extends DataBinder {

	private String namePrefix;

	/**
	 * @param target the target into which properties are bound
	 */
	public RelaxedDataBinder(Object target) {
		super(wrapTarget(target));
	}

	private static Object wrapTarget(Object target) {
		if (target instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) target;
			target = new MapHolder(map);
		}
		return target;
	}

	/**
	 * @param target the target into which properties are bound
	 * @param namePrefix An optional prefix to be used when reading properties
	 */
	public RelaxedDataBinder(Object target, String namePrefix) {
		super(wrapTarget(target), (StringUtils.hasLength(namePrefix) ? namePrefix
				: DEFAULT_OBJECT_NAME));
		this.namePrefix = (StringUtils.hasLength(namePrefix) ? namePrefix + "." : null);
	}

	@Override
	protected void doBind(MutablePropertyValues propertyValues) {
		propertyValues = modifyProperties(propertyValues, getTarget());
		// Harmless additional property editor comes in very handy sometimes...
		getPropertyEditorRegistry().registerCustomEditor(InetAddress.class,
				new InetAddressEditor());
		super.doBind(propertyValues);
	}

	/**
	 * Modify the property values so that period separated property paths are valid for
	 * map keys. Also creates new maps for properties of map type that are null (assuming
	 * all maps are potentially nested). The standard bracket <code>[...]</code>
	 * dereferencing is also accepted.
	 * 
	 * @param propertyValues the property values
	 * @param target the target object
	 */
	private MutablePropertyValues modifyProperties(MutablePropertyValues propertyValues,
			Object target) {

		propertyValues = getProperyValuesForNamePrefix(propertyValues);

		if (target instanceof MapHolder) {
			propertyValues = addMapPrefix(propertyValues);
		}

		BeanWrapper targetWrapper = new BeanWrapperImpl(target);
		targetWrapper.setAutoGrowNestedPaths(true);

		List<PropertyValue> list = propertyValues.getPropertyValueList();
		for (int i = 0; i < list.size(); i++) {
			modifyProperty(propertyValues, targetWrapper, list.get(i), i);
		}
		return propertyValues;
	}

	private MutablePropertyValues addMapPrefix(MutablePropertyValues propertyValues) {
		MutablePropertyValues rtn = new MutablePropertyValues();
		for (PropertyValue pv : propertyValues.getPropertyValues()) {
			rtn.add("map." + pv.getName(), pv.getValue());
		}
		return rtn;
	}

	private MutablePropertyValues getProperyValuesForNamePrefix(
			MutablePropertyValues propertyValues) {
		if (this.namePrefix == null) {
			return propertyValues;
		}
		MutablePropertyValues rtn = new MutablePropertyValues();
		for (PropertyValue pv : propertyValues.getPropertyValues()) {
			String name = pv.getName();
			if (name.startsWith(this.namePrefix)) {
				name = name.substring(this.namePrefix.length());
				rtn.add(name, pv.getValue());
			}
		}
		return rtn;
	}

	private void modifyProperty(MutablePropertyValues propertyValues, BeanWrapper target,
			PropertyValue propertyValue, int index) {
		String oldName = propertyValue.getName();
		String name = normalizePath(target, oldName);
		if (!name.equals(oldName)) {
			propertyValues.setPropertyValueAt(
					new PropertyValue(name, propertyValue.getValue()), index);
		}
	}

	/**
	 * Normalize a bean property path to a format understood by a BeanWrapper. This is
	 * used so that
	 * <ul>
	 * <li>Fuzzy matching can be employed for bean property names</li>
	 * <li>Period separators can be used instead of indexing ([...]) for map keys</li>
	 * </ul>
	 * 
	 * @param wrapper a bean wrapper for the object to bind
	 * @param path the bean path to bind
	 * @return a transformed path with correct bean wrapper syntax
	 */
	protected String normalizePath(BeanWrapper wrapper, String path) {
		return initializePath(wrapper, new BeanPath(path), 0);
	}

	private String initializePath(BeanWrapper wrapper, BeanPath path, int index) {

		String prefix = path.prefix(index);
		String key = path.name(index);
		if (path.isProperty(index)) {
			key = getActualPropertyName(wrapper, prefix, key);
			path.rename(index, key);
		}
		if (path.name(++index) == null) {
			return path.toString();
		}

		String name = path.prefix(index);
		TypeDescriptor descriptor = wrapper.getPropertyTypeDescriptor(name);
		if (descriptor == null || descriptor.isMap()) {
			if (descriptor != null) {
				wrapper.getPropertyValue(name + "[foo]");
			}
			path.mapIndex(index);
			extendMapIfNecessary(wrapper, path, index);
		}
		else if (descriptor.isCollection()) {
			extendCollectionIfNecessary(wrapper, path, index);
		}
		else if (descriptor.getType().equals(Object.class)) {
			path.mapIndex(index);
			String next = path.prefix(index + 1);
			if (wrapper.getPropertyValue(next) == null) {
				wrapper.setPropertyValue(next, new LinkedHashMap<String, Object>());
			}
		}

		return initializePath(wrapper, path, index);

	}

	private void extendCollectionIfNecessary(BeanWrapper wrapper, BeanPath path, int index) {
		String name = path.prefix(index);
		TypeDescriptor elementDescriptor = wrapper.getPropertyTypeDescriptor(name)
				.getElementTypeDescriptor();
		if (!elementDescriptor.isMap() && !elementDescriptor.isCollection()
				&& !elementDescriptor.getType().equals(Object.class)) {
			return;
		}
		Object extend = new LinkedHashMap<String, Object>();
		if (!elementDescriptor.isMap() && path.isArrayIndex(index + 1)) {
			extend = new ArrayList<Object>();
		}
		wrapper.setPropertyValue(path.prefix(index + 1), extend);
	}

	private void extendMapIfNecessary(BeanWrapper wrapper, BeanPath path, int index) {
		String name = path.prefix(index);
		TypeDescriptor parent = wrapper.getPropertyTypeDescriptor(name);
		if (parent == null) {
			return;
		}
		TypeDescriptor descriptor = parent.getMapValueTypeDescriptor();
		if (!descriptor.isMap() && !descriptor.isCollection()
				&& !descriptor.getType().equals(Object.class)) {
			return;
		}
		Object extend = new LinkedHashMap<String, Object>();
		if (descriptor.isCollection()) {
			extend = new ArrayList<Object>();
		}
		wrapper.setPropertyValue(path.prefix(index + 1), extend);
	}

	private String getActualPropertyName(BeanWrapper target, String prefix, String name) {
		prefix = StringUtils.hasText(prefix) ? prefix + "." : "";
		for (Variation variation : Variation.values()) {
			for (Manipulation manipulation : Manipulation.values()) {
				// Apply all manipulations before attempting variations
				String candidate = variation.apply(manipulation.apply(name));
				try {
					if (target.getPropertyType(prefix + candidate) != null) {
						return candidate;
					}
				}
				catch (InvalidPropertyException ex) {
					// swallow and continue
				}
			}
		}
		return name;
	}

	static enum Variation {
		NONE {
			@Override
			public String apply(String value) {
				return value;
			}
		},
		UPPERCASE {
			@Override
			public String apply(String value) {
				return value.toUpperCase();
			}
		},

		LOWERCASE {
			@Override
			public String apply(String value) {
				return value.toLowerCase();
			}
		};

		public abstract String apply(String value);
	}

	static enum Manipulation {
		NONE {
			@Override
			public String apply(String value) {
				return value;
			}
		},
		UNDERSCORE {
			@Override
			public String apply(String value) {
				return value.replace("-", "_");
			}
		},

		CAMELCASE {
			@Override
			public String apply(String value) {
				StringBuilder builder = new StringBuilder();
				for (String field : UNDERSCORE.apply(value).split("_")) {
					builder.append(builder.length() == 0 ? field : StringUtils
							.capitalize(field));
				}
				return builder.toString();
			}
		};

		public abstract String apply(String value);
	}

	static class MapHolder {
		private Map<String, Object> map;

		public MapHolder(Map<String, Object> map) {
			this.map = map;
		}

		public void setMap(Map<String, Object> map) {
			this.map = map;
		}

		public Map<String, Object> getMap() {
			return this.map;
		}
	}

	private static class BeanPath {

		private List<PathNode> nodes;

		public BeanPath(String path) {
			this.nodes = splitPath(path);
		}

		public void mapIndex(int index) {
			PathNode node = this.nodes.get(index);
			if (node instanceof PropertyNode) {
				node = ((PropertyNode) node).mapIndex();
			}
			this.nodes.set(index, node);
		}

		public String prefix(int index) {
			return range(0, index);
		}

		public void rename(int index, String name) {
			this.nodes.get(index).name = name;
		}

		public String name(int index) {
			if (index < this.nodes.size()) {
				return this.nodes.get(index).name;
			}
			return null;
		}

		private String range(int start, int end) {
			StringBuilder builder = new StringBuilder();
			for (int i = start; i < end; i++) {
				PathNode node = this.nodes.get(i);
				builder.append(node);
			}
			if (builder.toString().startsWith(("."))) {
				builder.replace(0, 1, "");
			}
			return builder.toString();
		}

		public boolean isArrayIndex(int index) {
			return this.nodes.get(index) instanceof ArrayIndexNode;
		}

		public boolean isProperty(int index) {
			return this.nodes.get(index) instanceof PropertyNode;
		}

		@Override
		public String toString() {
			return prefix(this.nodes.size());
		}

		private static class PathNode {

			protected String name;

			public PathNode(String name) {
				this.name = name;
			}

		}

		private static class ArrayIndexNode extends PathNode {

			public ArrayIndexNode(String name) {
				super(name);
			}

			@Override
			public String toString() {
				return "[" + this.name + "]";
			}

		}

		private static class MapIndexNode extends PathNode {

			public MapIndexNode(String name) {
				super(name);
			}

			@Override
			public String toString() {
				return "[" + this.name + "]";
			}
		}

		private static class PropertyNode extends PathNode {

			public PropertyNode(String name) {
				super(name);
			}

			public MapIndexNode mapIndex() {
				return new MapIndexNode(this.name);
			}

			@Override
			public String toString() {
				return "." + this.name;
			}
		}

		private List<PathNode> splitPath(String path) {
			List<PathNode> nodes = new ArrayList<PathNode>();
			for (String name : StringUtils.delimitedListToStringArray(path, ".")) {
				for (String sub : StringUtils.delimitedListToStringArray(name, "[")) {
					if (StringUtils.hasText(sub)) {
						if (sub.endsWith("]")) {
							sub = sub.substring(0, sub.length() - 1);
							if (sub.matches("[0-9]+")) {
								nodes.add(new ArrayIndexNode(sub));
							}
							else {
								nodes.add(new MapIndexNode(sub));
							}
						}
						else {
							nodes.add(new PropertyNode(sub));
						}
					}
				}
			}
			return nodes;
		}

	}

}
