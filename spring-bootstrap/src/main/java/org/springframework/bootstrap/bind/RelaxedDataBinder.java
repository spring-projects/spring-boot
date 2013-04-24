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
		super(target);
	}

	/**
	 * @param target the target into which properties are bound
	 * @param namePrefix An optional prefix to be used when reading properties
	 */
	public RelaxedDataBinder(Object target, String namePrefix) {
		super(target, (StringUtils.hasLength(namePrefix) ? namePrefix
				: DEFAULT_OBJECT_NAME));
		this.namePrefix = (StringUtils.hasLength(namePrefix) ? namePrefix + "." : null);
	}

	@Override
	protected void doBind(MutablePropertyValues propertyValues) {
		propertyValues = modifyProperties(propertyValues, getTarget());
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

		BeanWrapper targetWrapper = new BeanWrapperImpl(target);
		targetWrapper.setAutoGrowNestedPaths(true);

		propertyValues = getProperyValuesForNamePrefix(propertyValues);

		List<PropertyValue> list = propertyValues.getPropertyValueList();
		for (int i = 0; i < list.size(); i++) {
			modifyProperty(propertyValues, targetWrapper, list.get(i), i);
		}
		return propertyValues;
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
				String suffix = name.substring(base.length());
				Map<String, Object> nested = new LinkedHashMap<String, Object>();
				if (target.getPropertyValue(base) != null) {
					@SuppressWarnings("unchecked")
					Map<String, Object> existing = (Map<String, Object>) target
							.getPropertyValue(base);
					nested = existing;
				} else {
					target.setPropertyValue(base, nested);
				}
				Map<String, Object> value = nested;
				nested = new LinkedHashMap<String, Object>();
				String[] tree = StringUtils.delimitedListToStringArray(suffix, ".");
				for (int j = 1; j < tree.length - 1; j++) {
					if (!value.containsKey(tree[j])) {
						value.put(tree[j], nested);
					}
					value = nested;
					nested = new LinkedHashMap<String, Object>();
				}
				String refName = base + suffix.replaceAll("\\.([a-zA-Z0-9]*)", "[$1]");
				propertyValues.setPropertyValueAt(new PropertyValue(refName,
						propertyValue.getValue()), index);
				break;
			}
		}
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

}
