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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
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

		String name = propertyValue.getName();
		StringBuilder builder = new StringBuilder();
		Class<?> type = target.getWrappedClass();

		for (String key : StringUtils.delimitedListToStringArray(name, ".")) {
			if (builder.length() != 0) {
				builder.append(".");
			}
			String oldKey = key;
			key = getActualPropertyName(target, builder.toString(), oldKey);
			builder.append(key);
			String base = builder.toString();
			if (!oldKey.equals(key)) {
				propertyValues.setPropertyValueAt(
						new PropertyValue(base, propertyValue.getValue()), index);
			}
			type = target.getPropertyType(base);

			// Any nested properties that are maps, are assumed to be simple nested
			// maps of maps...
			if (type != null && Map.class.isAssignableFrom(type)) {
				Map<String, Object> nested = new LinkedHashMap<String, Object>();
				if (target.getPropertyValue(base) != null) {
					@SuppressWarnings("unchecked")
					Map<String, Object> existing = (Map<String, Object>) target
							.getPropertyValue(base);
					nested = existing;
				} else {
					target.setPropertyValue(base, nested);
				}
				modifyPopertiesForMap(nested, propertyValues, index, base);
				break;
			}
		}
	}

	private void modifyPopertiesForMap(Map<String, Object> target,
			MutablePropertyValues propertyValues, int index, String base) {
		PropertyValue propertyValue = propertyValues.getPropertyValueList().get(index);
		String name = propertyValue.getName();
		String suffix = name.substring(base.length());
		Map<String, Object> value = new LinkedHashMap<String, Object>();
		String[] tree = StringUtils.delimitedListToStringArray(
				suffix.startsWith(".") ? suffix.substring(1) : suffix, ".");
		for (int j = 0; j < tree.length - 1; j++) {
			if (!target.containsKey(tree[j])) {
				target.put(tree[j], value);
			}
			target = value;
			value = new LinkedHashMap<String, Object>();
		}
		String refName = base + suffix.replaceAll("\\.([a-zA-Z0-9]*)", "[$1]");
		propertyValues.setPropertyValueAt(
				new PropertyValue(refName, propertyValue.getValue()), index);

	}

	private String getActualPropertyName(BeanWrapper target, String prefix, String name) {
		for (Variation variation : Variation.values()) {
			for (Manipulation manipulation : Manipulation.values()) {
				// Apply all manipulations before attempting variations
				String candidate = variation.apply(manipulation.apply(name));
				try {
					if (target.getPropertyType(prefix + candidate) != null) {
						return candidate;
					}
				} catch (InvalidPropertyException ex) {
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

}
