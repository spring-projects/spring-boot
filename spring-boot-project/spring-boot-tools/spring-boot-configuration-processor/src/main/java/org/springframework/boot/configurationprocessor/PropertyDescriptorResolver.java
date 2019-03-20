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

package org.springframework.boot.configurationprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Resolve {@link PropertyDescriptor} instances.
 *
 * @author Stephane Nicoll
 */
class PropertyDescriptorResolver {

	private final MetadataGenerationEnvironment environment;

	PropertyDescriptorResolver(MetadataGenerationEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@link PropertyDescriptor} instances that are valid candidates for the
	 * specified {@link TypeElement type} based on the specified {@link ExecutableElement
	 * factory method}, if any.
	 * @param type the target type
	 * @param factoryMethod the method that triggered the metadata for that {@code type}
	 * or {@code null}
	 * @return the candidate properties for metadata generation
	 */
	public Stream<PropertyDescriptor<?>> resolve(TypeElement type,
			ExecutableElement factoryMethod) {
		TypeElementMembers members = new TypeElementMembers(this.environment, type);
		List<PropertyDescriptor<?>> candidates = new ArrayList<>();
		// First check if we have regular java bean properties there
		members.getPublicGetters().forEach((name, getter) -> {
			TypeMirror returnType = getter.getReturnType();
			candidates.add(new JavaBeanPropertyDescriptor(type, factoryMethod, getter,
					name, returnType, members.getFields().get(name),
					members.getPublicSetter(name, returnType)));
		});
		// Then check for Lombok ones
		members.getFields().forEach((name, field) -> {
			TypeMirror returnType = field.asType();
			ExecutableElement getter = members.getPublicGetter(name, returnType);
			ExecutableElement setter = members.getPublicSetter(name, returnType);
			candidates.add(new LombokPropertyDescriptor(type, factoryMethod, field, name,
					returnType, getter, setter));
		});
		return candidates.stream().filter(this::isCandidate);
	}

	private boolean isCandidate(PropertyDescriptor<?> descriptor) {
		return descriptor.isProperty(this.environment)
				|| descriptor.isNested(this.environment);
	}

}
