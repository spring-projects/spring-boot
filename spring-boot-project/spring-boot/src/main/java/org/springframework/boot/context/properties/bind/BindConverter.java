/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.FileEditor;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.util.CollectionUtils;

/**
 * Utility to handle any conversion needed during binding. This class is not thread-safe
 * and so a new instance is created for each top-level bind.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class BindConverter {

	private static BindConverter sharedInstance;

	private final List<ConversionService> delegates;

	private BindConverter(List<ConversionService> conversionServices,
			Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
		List<ConversionService> delegates = new ArrayList<>();
		delegates.add(new TypeConverterConversionService(propertyEditorInitializer));
		boolean hasApplication = false;
		if (!CollectionUtils.isEmpty(conversionServices)) {
			for (ConversionService conversionService : conversionServices) {
				delegates.add(conversionService);
				hasApplication = hasApplication || conversionService instanceof ApplicationConversionService;
			}
		}
		if (!hasApplication) {
			delegates.add(ApplicationConversionService.getSharedInstance());
		}
		this.delegates = Collections.unmodifiableList(delegates);
	}

	boolean canConvert(Object source, ResolvableType targetType, Annotation... targetAnnotations) {
		return canConvert(TypeDescriptor.forObject(source),
				new ResolvableTypeDescriptor(targetType, targetAnnotations));
	}

	private boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		for (ConversionService service : this.delegates) {
			if (service.canConvert(sourceType, targetType)) {
				return true;
			}
		}
		return false;
	}

	<T> T convert(Object source, Bindable<T> target) {
		return convert(source, target.getType(), target.getAnnotations());
	}

	@SuppressWarnings("unchecked")
	<T> T convert(Object source, ResolvableType targetType, Annotation... targetAnnotations) {
		if (source == null) {
			return null;
		}
		return (T) convert(source, TypeDescriptor.forObject(source),
				new ResolvableTypeDescriptor(targetType, targetAnnotations));
	}

	private Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		ConversionException failure = null;
		for (ConversionService delegate : this.delegates) {
			try {
				if (delegate.canConvert(sourceType, targetType)) {
					return delegate.convert(source, sourceType, targetType);
				}
			}
			catch (ConversionException ex) {
				if (failure == null && ex instanceof ConversionFailedException) {
					failure = ex;
				}
			}
		}
		throw (failure != null) ? failure : new ConverterNotFoundException(sourceType, targetType);
	}

	static BindConverter get(List<ConversionService> conversionServices,
			Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
		boolean sharedApplicationConversionService = (conversionServices == null) || (conversionServices.size() == 1
				&& conversionServices.get(0) == ApplicationConversionService.getSharedInstance());
		if (propertyEditorInitializer == null && sharedApplicationConversionService) {
			return getSharedInstance();
		}
		return new BindConverter(conversionServices, propertyEditorInitializer);
	}

	private static BindConverter getSharedInstance() {
		if (sharedInstance == null) {
			sharedInstance = new BindConverter(null, null);
		}
		return sharedInstance;
	}

	/**
	 * A {@link TypeDescriptor} backed by a {@link ResolvableType}.
	 */
	private static class ResolvableTypeDescriptor extends TypeDescriptor {

		ResolvableTypeDescriptor(ResolvableType resolvableType, Annotation[] annotations) {
			super(resolvableType, null, annotations);
		}

	}

	/**
	 * A {@link ConversionService} implementation that delegates to a
	 * {@link SimpleTypeConverter}. Allows {@link PropertyEditor} based conversion for
	 * simple types, arrays and collections.
	 */
	private static class TypeConverterConversionService extends GenericConversionService {

		TypeConverterConversionService(Consumer<PropertyEditorRegistry> initializer) {
			addConverter(new TypeConverterConverter(initializer));
			ApplicationConversionService.addDelimitedStringConverters(this);
		}

		@Override
		public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
			// Prefer conversion service to handle things like String to char[].
			if (targetType.isArray() && targetType.getElementTypeDescriptor().isPrimitive()) {
				return false;
			}
			return super.canConvert(sourceType, targetType);
		}

	}

	/**
	 * {@link ConditionalGenericConverter} that delegates to {@link SimpleTypeConverter}.
	 */
	private static class TypeConverterConverter implements ConditionalGenericConverter {

		private static final Set<Class<?>> EXCLUDED_EDITORS;
		static {
			Set<Class<?>> excluded = new HashSet<>();
			excluded.add(CustomNumberEditor.class);
			excluded.add(CustomBooleanEditor.class);
			excluded.add(FileEditor.class);
			EXCLUDED_EDITORS = Collections.unmodifiableSet(excluded);
		}

		private final Consumer<PropertyEditorRegistry> initializer;

		// SimpleTypeConverter is not thread-safe to use for conversion but we can use it
		// in a thread-safe way to check if conversion is possible.
		private final SimpleTypeConverter matchesOnlyTypeConverter;

		TypeConverterConverter(Consumer<PropertyEditorRegistry> initializer) {
			this.initializer = initializer;
			this.matchesOnlyTypeConverter = createTypeConverter();
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, Object.class));
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			Class<?> type = targetType.getType();
			if (type == null || type == Object.class || Collection.class.isAssignableFrom(type)
					|| Map.class.isAssignableFrom(type)) {
				return false;
			}
			PropertyEditor editor = this.matchesOnlyTypeConverter.getDefaultEditor(type);
			if (editor == null) {
				editor = this.matchesOnlyTypeConverter.findCustomEditor(type, null);
			}
			if (editor == null && String.class != type) {
				editor = BeanUtils.findEditorByConvention(type);
			}
			return (editor != null && !EXCLUDED_EDITORS.contains(editor.getClass()));
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return createTypeConverter().convertIfNecessary(source, targetType.getType());
		}

		private SimpleTypeConverter createTypeConverter() {
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			if (this.initializer != null) {
				this.initializer.accept(typeConverter);
			}
			return typeConverter;
		}

	}

}
