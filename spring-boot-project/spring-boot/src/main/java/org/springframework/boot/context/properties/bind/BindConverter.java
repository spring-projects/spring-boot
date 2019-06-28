/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.beans.propertyeditors.FileEditor;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.util.Assert;

/**
 * Utility to handle any conversion needed during binding. This class is not thread-safe
 * and so a new instance is created for each top-level bind.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class BindConverter {

	private static final Set<Class<?>> EXCLUDED_EDITORS;
	static {
		Set<Class<?>> excluded = new HashSet<>();
		excluded.add(FileEditor.class); // gh-12163
		EXCLUDED_EDITORS = Collections.unmodifiableSet(excluded);
	}

	private static BindConverter sharedInstance;

	private final ConversionService conversionService;

	private BindConverter(ConversionService conversionService,
			Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		List<ConversionService> conversionServices = getConversionServices(conversionService,
				propertyEditorInitializer);
		this.conversionService = new CompositeConversionService(conversionServices);
	}

	private List<ConversionService> getConversionServices(ConversionService conversionService,
			Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
		List<ConversionService> services = new ArrayList<>();
		services.add(new TypeConverterConversionService(propertyEditorInitializer));
		services.add(conversionService);
		if (!(conversionService instanceof ApplicationConversionService)) {
			services.add(ApplicationConversionService.getSharedInstance());
		}
		return services;
	}

	public boolean canConvert(Object value, ResolvableType type, Annotation... annotations) {
		return this.conversionService.canConvert(TypeDescriptor.forObject(value),
				new ResolvableTypeDescriptor(type, annotations));
	}

	public <T> T convert(Object result, Bindable<T> target) {
		return convert(result, target.getType(), target.getAnnotations());
	}

	@SuppressWarnings("unchecked")
	public <T> T convert(Object value, ResolvableType type, Annotation... annotations) {
		if (value == null) {
			return null;
		}
		return (T) this.conversionService.convert(value, TypeDescriptor.forObject(value),
				new ResolvableTypeDescriptor(type, annotations));
	}

	static BindConverter get(ConversionService conversionService,
			Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
		if (conversionService == ApplicationConversionService.getSharedInstance()
				&& propertyEditorInitializer == null) {
			if (sharedInstance == null) {
				sharedInstance = new BindConverter(conversionService, propertyEditorInitializer);
			}
			return sharedInstance;
		}
		return new BindConverter(conversionService, propertyEditorInitializer);
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
	 * Composite {@link ConversionService} used to call multiple services.
	 */
	static class CompositeConversionService implements ConversionService {

		private final List<ConversionService> delegates;

		CompositeConversionService(List<ConversionService> delegates) {
			this.delegates = delegates;
		}

		@Override
		public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
			Assert.notNull(targetType, "Target type to convert to cannot be null");
			return canConvert((sourceType != null) ? TypeDescriptor.valueOf(sourceType) : null,
					TypeDescriptor.valueOf(targetType));
		}

		@Override
		public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
			for (ConversionService service : this.delegates) {
				if (service.canConvert(sourceType, targetType)) {
					return true;
				}
			}
			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T convert(Object source, Class<T> targetType) {
			Assert.notNull(targetType, "Target type to convert to cannot be null");
			return (T) convert(source, TypeDescriptor.forObject(source), TypeDescriptor.valueOf(targetType));
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			for (int i = 0; i < this.delegates.size() - 1; i++) {
				try {
					ConversionService delegate = this.delegates.get(i);
					if (delegate.canConvert(sourceType, targetType)) {
						return delegate.convert(source, sourceType, targetType);
					}
				}
				catch (ConversionException ex) {
				}
			}
			return this.delegates.get(this.delegates.size() - 1).convert(source, sourceType, targetType);
		}

	}

	/**
	 * A {@link ConversionService} implementation that delegates to a
	 * {@link SimpleTypeConverter}. Allows {@link PropertyEditor} based conversion for
	 * simple types, arrays and collections.
	 */
	private static class TypeConverterConversionService extends GenericConversionService {

		TypeConverterConversionService(Consumer<PropertyEditorRegistry> initializer) {
			addConverter(new TypeConverterConverter(createTypeConverter(initializer)));
			ApplicationConversionService.addDelimitedStringConverters(this);
		}

		private SimpleTypeConverter createTypeConverter(Consumer<PropertyEditorRegistry> initializer) {
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			if (initializer != null) {
				initializer.accept(typeConverter);
			}
			return typeConverter;
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

		private final SimpleTypeConverter typeConverter;

		TypeConverterConverter(SimpleTypeConverter typeConverter) {
			this.typeConverter = typeConverter;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, Object.class));
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return getPropertyEditor(targetType.getType()) != null;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			SimpleTypeConverter typeConverter = this.typeConverter;
			return typeConverter.convertIfNecessary(source, targetType.getType());
		}

		private PropertyEditor getPropertyEditor(Class<?> type) {
			if (type == null || type == Object.class || Collection.class.isAssignableFrom(type)
					|| Map.class.isAssignableFrom(type)) {
				return null;
			}
			SimpleTypeConverter typeConverter = this.typeConverter;
			PropertyEditor editor = typeConverter.getDefaultEditor(type);
			if (editor == null) {
				editor = typeConverter.findCustomEditor(type, null);
			}
			if (editor == null && String.class != type) {
				editor = BeanUtils.findEditorByConvention(type);
			}
			if (editor == null || EXCLUDED_EDITORS.contains(editor.getClass())) {
				return null;
			}
			return editor;
		}

	}

}
