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

package org.springframework.boot.context.properties.bind;

import java.lang.annotation.Annotation;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * A {@link TypeDescriptor} backed by a {@link ResolvableType}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("serial")
final class ResolvableTypeDescriptor extends TypeDescriptor {

	private ResolvableTypeDescriptor(ResolvableType resolvableType,
			Annotation[] annotations) {
		super(resolvableType, null, annotations);
	}

	/**
	 * Determine if the specified source object can be converted to this type.
	 * @param conversionService the backing conversion service
	 * @param source the source to check
	 * @return {@code true} if conversion can be performed
	 */
	public boolean canConvert(ConversionService conversionService, Object source) {
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		return conversionService.canConvert(sourceType, this);
	}

	/**
	 * Convert the given source object into this type.
	 * @param conversionService the source conversion service
	 * @param value the value to convert
	 * @param <T> the target type
	 * @return the converted value
	 * @throws ConversionException if a conversion exception occurred
	 */
	@SuppressWarnings("unchecked")
	public <T> T convert(ConversionService conversionService, Object value) {
		if (value == null) {
			return null;
		}
		TypeDescriptor sourceType = TypeDescriptor.forObject(value);
		return (T) conversionService.convert(value, sourceType, this);
	}

	/**
	 * Create a {@link TypeDescriptor} for the specified {@link Bindable}.
	 * @param bindable the bindable
	 * @return the type descriptor
	 */
	public static ResolvableTypeDescriptor forBindable(Bindable<?> bindable) {
		return forType(bindable.getType(), bindable.getAnnotations());
	}

	/**
	 * Return a {@link TypeDescriptor} for the specified {@link ResolvableType}.
	 * @param type the resolvable type
	 * @param annotations the annotations to include
	 * @return the type descriptor
	 */
	public static ResolvableTypeDescriptor forType(ResolvableType type,
			Annotation... annotations) {
		return new ResolvableTypeDescriptor(type, annotations);
	}

}
