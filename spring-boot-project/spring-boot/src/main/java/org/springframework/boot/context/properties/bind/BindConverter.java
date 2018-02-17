/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.beans.PropertyEditor;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

/**
 * Utility to handle any conversion needed during binding. This class is not thread-safe
 * and so a new instance is created for each top-level bind.
 *
 * @author Phillip Webb
 */
class BindConverter {

	private final ConversionService conversionService;

	private final SimpleTypeConverter simpleTypeConverter;

	BindConverter(ConversionService conversionService,
			Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
		this.simpleTypeConverter = new SimpleTypeConverter();
		if (propertyEditorInitializer != null) {
			propertyEditorInitializer.accept(this.simpleTypeConverter);
		}
	}

	public boolean canConvert(Object value, ResolvableType type,
			Annotation... annotations) {
		return getPropertyEditor(type.resolve()) != null
				|| this.conversionService.canConvert(TypeDescriptor.forObject(value),
						new ResolvableTypeDescriptor(type, annotations));
	}

	public <T> T convert(Object result, Bindable<T> target) {
		return convert(result, target.getType(), target.getAnnotations());
	}

	@SuppressWarnings("unchecked")
	public <T> T convert(Object value, ResolvableType type, Annotation... annotations) {
		PropertyEditor propertyEditor = getPropertyEditor(type.resolve());
		if (propertyEditor != null) {
			if (value == null) {
				return null;
			}
			return (T) this.simpleTypeConverter.convertIfNecessary(value, type.resolve());
		}
		return (T) this.conversionService.convert(value, TypeDescriptor.forObject(value),
				new ResolvableTypeDescriptor(type, annotations));
	}

	private PropertyEditor getPropertyEditor(Class<?> type) {
		if (type == null || type == Object.class
				|| Collection.class.isAssignableFrom(type)
				|| Map.class.isAssignableFrom(type)) {
			return null;
		}
		PropertyEditor editor = this.simpleTypeConverter.getDefaultEditor(type);
		if (editor == null) {
			editor = this.simpleTypeConverter.findCustomEditor(type, null);
		}
		if (editor == null && String.class != type) {
			editor = BeanUtils.findEditorByConvention(type);
		}
		return editor;
	}

	/**
	 * A {@link TypeDescriptor} backed by a {@link ResolvableType}.
	 */
	final class ResolvableTypeDescriptor extends TypeDescriptor {

		ResolvableTypeDescriptor(ResolvableType resolvableType,
				Annotation[] annotations) {
			super(resolvableType, null, annotations);
		}

	}
}
