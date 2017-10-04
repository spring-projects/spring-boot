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

import java.lang.annotation.Annotation;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.ResolvableType;
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
	 * Create a {@link TypeDescriptor} for the specified {@link Bindable}.
	 * @param bindable the bindable
	 * @return the type descriptor
	 */
	public static TypeDescriptor forBindable(Bindable<?> bindable) {
		return forType(bindable.getType(), bindable.getAnnotations());
	}

	/**
	 * Return a {@link TypeDescriptor} for the specified {@link ResolvableType}.
	 * @param type the resolvable type
	 * @param annotations the annotations to include
	 * @return the type descriptor
	 */
	public static TypeDescriptor forType(ResolvableType type, Annotation... annotations) {
		return new ResolvableTypeDescriptor(type, annotations);
	}

}
