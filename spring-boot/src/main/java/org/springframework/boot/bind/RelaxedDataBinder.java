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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

/**
 * Binder implementation that allows caller to bind to maps and also allows property names
 * to match a bit loosely (if underscores or dashes are removed and replaced with camel
 * case for example).
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @see RelaxedNames
 */
public class RelaxedDataBinder extends DataBinder {

	private String namePrefix;

	private boolean ignoreNestedProperties;

	private ConversionService relaxedConversionService;

	/**
	 * Create a new {@link RelaxedDataBinder} instance.
	 * @param target the target into which properties are bound
	 */
	public RelaxedDataBinder(Object target) {
		super(wrapTarget(target));
	}

	/**
	 * Create a new {@link RelaxedDataBinder} instance.
	 * @param target the target into which properties are bound
	 * @param namePrefix An optional prefix to be used when reading properties
	 */
	public RelaxedDataBinder(Object target, String namePrefix) {
		super(wrapTarget(target), (StringUtils.hasLength(namePrefix) ? namePrefix
				: DEFAULT_OBJECT_NAME));
		this.namePrefix = (StringUtils.hasLength(namePrefix) ? namePrefix + "." : null);
	}

	/**
	 * Flag to disable binding of nested properties (i.e. those with period separators in
	 * their paths). Can be useful to disable this if the name prefix is empty and you
	 * don't want to ignore unknown fields.
	 * @param ignoreNestedProperties the flag to set (default false)
	 */
	public void setIgnoreNestedProperties(boolean ignoreNestedProperties) {
		this.ignoreNestedProperties = ignoreNestedProperties;
	}

	@Override
	public void setConversionService(ConversionService conversionService) {
		super.setConversionService(conversionService);
		this.relaxedConversionService = new RelaxedConversionService(getConversionService());
	}

	@Override
	public void initBeanPropertyAccess() {
		super.initBeanPropertyAccess();
		this.relaxedConversionService = (this.relaxedConversionService != null
				?  this.relaxedConversionService : new RelaxedConversionService(getConversionService()));
		// Hook in the RelaxedConversionService
		getInternalBindingResult().initConversion(relaxedConversionService);
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
	 * @param propertyValues the property values
	 * @param target the target object
	 */
	private MutablePropertyValues modifyProperties(MutablePropertyValues propertyValues,
			Object target) {

		propertyValues = getPropertyValuesForNamePrefix(propertyValues);

		if (target instanceof MapHolder) {
			propertyValues = addMapPrefix(propertyValues);
		}

		BeanWrapper targetWrapper = new BeanWrapperImpl(target);
		targetWrapper.setConversionService(this.relaxedConversionService);
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

	private MutablePropertyValues getPropertyValuesForNamePrefix(
			MutablePropertyValues propertyValues) {
		if (!StringUtils.hasText(this.namePrefix) && !this.ignoreNestedProperties) {
			return propertyValues;
		}
		MutablePropertyValues rtn = new MutablePropertyValues();
		for (PropertyValue value : propertyValues.getPropertyValues()) {
			String name = value.getName();
			for (String candidate : new RelaxedNames(this.namePrefix)) {
				if (name.startsWith(candidate)) {
					name = name.substring(candidate.length());
					if (!(this.ignoreNestedProperties && name.contains("."))) {
						rtn.add(name, value.getValue());
					}
				}
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
				TypeDescriptor valueDescriptor = descriptor.getMapValueTypeDescriptor();
				if (valueDescriptor != null) {
					Class<?> valueType = valueDescriptor.getObjectType();
					if (valueType != null
							&& CharSequence.class.isAssignableFrom(valueType)) {
						path.collapseKeys(index);
					}
				}
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
		if (descriptor == null) {
			descriptor = TypeDescriptor.valueOf(Object.class);
		}
		if (!descriptor.isMap() && !descriptor.isCollection()
				&& !descriptor.getType().equals(Object.class)) {
			return;
		}
		String extensionName = path.prefix(index + 1);
		if (wrapper.isReadableProperty(extensionName)) {
			Object currentValue = wrapper.getPropertyValue(extensionName);
			if ((descriptor.isCollection() && currentValue instanceof Collection)
					|| (!descriptor.isCollection() && currentValue instanceof Map)) {
				return;
			}
		}
		Object extend = new LinkedHashMap<String, Object>();
		if (descriptor.isCollection()) {
			extend = new ArrayList<Object>();
		}
		wrapper.setPropertyValue(extensionName, extend);
	}

	private String getActualPropertyName(BeanWrapper target, String prefix, String name) {
		prefix = StringUtils.hasText(prefix) ? prefix + "." : "";
		for (String candidate : new RelaxedNames(name)) {
			try {
				if (target.getPropertyType(prefix + candidate) != null) {
					return candidate;
				}
			}
			catch (InvalidPropertyException ex) {
				// swallow and continue
			}
		}
		return name;
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
	 * Holder to allow Map targets to be bound.
	 */
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

		public void collapseKeys(int index) {
			List<PathNode> revised = new ArrayList<PathNode>();
			for (int i = 0; i < index; i++) {
				revised.add(this.nodes.get(i));
			}
			StringBuilder builder = new StringBuilder();
			for (int i = index; i < this.nodes.size(); i++) {
				if (i > index) {
					builder.append(".");
				}
				builder.append(this.nodes.get(i).name);
			}
			revised.add(new PropertyNode(builder.toString()));
			this.nodes = revised;
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

	}

}
