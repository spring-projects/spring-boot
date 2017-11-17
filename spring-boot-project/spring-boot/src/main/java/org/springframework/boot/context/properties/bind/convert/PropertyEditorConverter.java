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

package org.springframework.boot.context.properties.bind.convert;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * {@link GenericConverter} that delegates to Java bean {@link PropertyEditor
 * PropertyEditors}.
 *
 * @author Phillip Webb
 */
class PropertyEditorConverter implements GenericConverter, ConditionalConverter {

	private static final Set<Class<?>> SKIPPED;

	static {
		Set<Class<?>> skipped = new LinkedHashSet<>();
		skipped.add(Collection.class);
		skipped.add(Map.class);
		SKIPPED = Collections.unmodifiableSet(skipped);
	}

	/**
	 * Registry that can be used to check if conversion is supported. Since
	 * {@link PropertyEditor PropertyEditors} are not thread safe this can't be used for
	 * actual conversion.
	 */
	private final PropertyEditorRegistrySupport registry = new SimpleTypeConverter();

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return null;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Class<?> type = targetType.getType();
		if (isSkipped(type)) {
			return false;
		}
		PropertyEditor editor = this.registry.getDefaultEditor(type);
		editor = (editor != null ? editor : BeanUtils.findEditorByConvention(type));
		return editor != null;
	}

	private boolean isSkipped(Class<?> type) {
		return SKIPPED.stream().anyMatch((c) -> c.isAssignableFrom(type));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		return new SimpleTypeConverter().convertIfNecessary(source, targetType.getType());
	}

}
