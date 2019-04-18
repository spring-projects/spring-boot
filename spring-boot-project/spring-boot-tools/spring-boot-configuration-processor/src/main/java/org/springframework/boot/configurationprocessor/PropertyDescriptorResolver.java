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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

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
		ExecutableElement constructor = resolveConstructor(type);
		if (constructor != null) {
			return resolveConstructorProperties(type, factoryMethod, members,
					constructor);
		}
		else {
			return resolveJavaBeanProperties(type, factoryMethod, members);
		}
	}

	public Stream<PropertyDescriptor<?>> resolveConstructorProperties(TypeElement type,
			ExecutableElement factoryMethod, TypeElementMembers members,
			ExecutableElement constructor) {
		Map<String, PropertyDescriptor<?>> candidates = new LinkedHashMap<>();
		constructor.getParameters().forEach((parameter) -> {
			String name = parameter.getSimpleName().toString();
			TypeMirror propertyType = parameter.asType();
			ExecutableElement getter = members.getPublicGetter(name, propertyType);
			ExecutableElement setter = members.getPublicSetter(name, propertyType);
			VariableElement field = members.getFields().get(name);
			register(candidates, new ConstructorParameterPropertyDescriptor(type,
					factoryMethod, parameter, name, propertyType, field, getter, setter));
		});
		return candidates.values().stream();
	}

	public Stream<PropertyDescriptor<?>> resolveJavaBeanProperties(TypeElement type,
			ExecutableElement factoryMethod, TypeElementMembers members) {
		// First check if we have regular java bean properties there
		Map<String, PropertyDescriptor<?>> candidates = new LinkedHashMap<>();
		members.getPublicGetters().forEach((name, getter) -> {
			TypeMirror propertyType = getter.getReturnType();
			register(candidates,
					new JavaBeanPropertyDescriptor(type, factoryMethod, getter, name,
							propertyType, members.getFields().get(name),
							members.getPublicSetter(name, propertyType)));
		});
		// Then check for Lombok ones
		members.getFields().forEach((name, field) -> {
			TypeMirror propertyType = field.asType();
			ExecutableElement getter = members.getPublicGetter(name, propertyType);
			ExecutableElement setter = members.getPublicSetter(name, propertyType);
			register(candidates, new LombokPropertyDescriptor(type, factoryMethod, field,
					name, propertyType, getter, setter));
		});
		return candidates.values().stream();
	}

	private void register(Map<String, PropertyDescriptor<?>> candidates,
			PropertyDescriptor<?> descriptor) {
		if (!candidates.containsKey(descriptor.getName()) && isCandidate(descriptor)) {
			candidates.put(descriptor.getName(), descriptor);
		}
	}

	private boolean isCandidate(PropertyDescriptor<?> descriptor) {
		return descriptor.isProperty(this.environment)
				|| descriptor.isNested(this.environment);
	}

	private ExecutableElement resolveConstructor(TypeElement type) {
		List<ExecutableElement> constructors = ElementFilter
				.constructorsIn(type.getEnclosedElements());
		if (constructors.size() == 1 && constructors.get(0).getParameters().size() > 0) {
			return constructors.get(0);
		}
		return null;
	}

}
