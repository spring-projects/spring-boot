/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Resolve {@link PropertyDescriptor} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Pavel Anisimov
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
	Stream<PropertyDescriptor> resolve(TypeElement type, ExecutableElement factoryMethod) {
		TypeElementMembers members = new TypeElementMembers(this.environment, type);
		if (factoryMethod != null) {
			return resolveJavaBeanProperties(type, members, factoryMethod);
		}
		return resolve(Bindable.of(type, this.environment), members);
	}

	private Stream<PropertyDescriptor> resolve(Bindable bindable, TypeElementMembers members) {
		if (bindable.isConstructorBindingEnabled()) {
			ExecutableElement bindConstructor = bindable.getBindConstructor();
			return (bindConstructor != null)
					? resolveConstructorBoundProperties(bindable.getType(), members, bindConstructor) : Stream.empty();
		}
		return resolveJavaBeanProperties(bindable.getType(), members, null);
	}

	private Stream<PropertyDescriptor> resolveConstructorBoundProperties(TypeElement declaringElement,
			TypeElementMembers members, ExecutableElement bindConstructor) {
		Map<String, PropertyDescriptor> candidates = new LinkedHashMap<>();
		bindConstructor.getParameters().forEach((parameter) -> {
			PropertyDescriptor descriptor = extracted(declaringElement, members, parameter);
			register(candidates, descriptor);
		});
		return candidates.values().stream();
	}

	private PropertyDescriptor extracted(TypeElement declaringElement, TypeElementMembers members,
			VariableElement parameter) {
		String name = getPropertyName(parameter);
		TypeMirror type = parameter.asType();
		ExecutableElement getter = members.getPublicGetter(name, type);
		ExecutableElement setter = members.getPublicSetter(name, type);
		VariableElement field = members.getFields().get(name);
		RecordComponentElement recordComponent = members.getRecordComponents().get(name);
		return (recordComponent != null)
				? new RecordParameterPropertyDescriptor(name, type, parameter, declaringElement, getter,
						recordComponent)
				: new ConstructorParameterPropertyDescriptor(name, type, parameter, declaringElement, getter, setter,
						field);
	}

	private String getPropertyName(VariableElement parameter) {
		return getPropertyName(parameter, parameter.getSimpleName().toString());
	}

	private String getPropertyName(VariableElement parameter, String fallback) {
		AnnotationMirror nameAnnotation = this.environment.getNameAnnotation(parameter);
		if (nameAnnotation != null) {
			return this.environment.getAnnotationElementStringValue(nameAnnotation, "value");
		}
		return fallback;
	}

	private Stream<PropertyDescriptor> resolveJavaBeanProperties(TypeElement declaringElement,
			TypeElementMembers members, ExecutableElement factoryMethod) {
		// First check if we have regular java bean properties there
		Map<String, PropertyDescriptor> candidates = new LinkedHashMap<>();
		members.getPublicGetters().forEach((name, getters) -> {
			VariableElement field = members.getFields().get(name);
			ExecutableElement getter = findMatchingGetter(members, getters, field);
			TypeMirror propertyType = getter.getReturnType();
			register(candidates, new JavaBeanPropertyDescriptor(getPropertyName(field, name), propertyType,
					declaringElement, getter, members.getPublicSetter(name, propertyType), field, factoryMethod));
		});
		// Then check for Lombok ones
		members.getFields().forEach((name, field) -> {
			TypeMirror propertyType = field.asType();
			ExecutableElement getter = members.getPublicGetter(name, propertyType);
			ExecutableElement setter = members.getPublicSetter(name, propertyType);
			register(candidates, new LombokPropertyDescriptor(getPropertyName(field, name), propertyType,
					declaringElement, getter, setter, field, factoryMethod));
		});
		return candidates.values().stream();
	}

	private ExecutableElement findMatchingGetter(TypeElementMembers members, List<ExecutableElement> candidates,
			VariableElement field) {
		if (candidates.size() > 1 && field != null) {
			return members.getMatchingGetter(candidates, field.asType());
		}
		return candidates.get(0);
	}

	private void register(Map<String, PropertyDescriptor> candidates, PropertyDescriptor descriptor) {
		if (!candidates.containsKey(descriptor.getName()) && isCandidate(descriptor)) {
			candidates.put(descriptor.getName(), descriptor);
		}
	}

	private boolean isCandidate(PropertyDescriptor descriptor) {
		return descriptor.isProperty(this.environment) || descriptor.isNested(this.environment);
	}

	/**
	 * Wrapper around a {@link TypeElement} that could be bound.
	 */
	private static class Bindable {

		private final TypeElement type;

		private final List<ExecutableElement> constructors;

		private final List<ExecutableElement> boundConstructors;

		Bindable(TypeElement type, List<ExecutableElement> constructors, List<ExecutableElement> boundConstructors) {
			this.type = type;
			this.constructors = constructors;
			this.boundConstructors = boundConstructors;
		}

		TypeElement getType() {
			return this.type;
		}

		boolean isConstructorBindingEnabled() {
			return !this.boundConstructors.isEmpty();
		}

		ExecutableElement getBindConstructor() {
			if (this.boundConstructors.isEmpty()) {
				return findBoundConstructor();
			}
			if (this.boundConstructors.size() == 1) {
				return this.boundConstructors.get(0);
			}
			return null;
		}

		private ExecutableElement findBoundConstructor() {
			ExecutableElement boundConstructor = null;
			for (ExecutableElement candidate : this.constructors) {
				if (!candidate.getParameters().isEmpty()) {
					if (boundConstructor != null) {
						return null;
					}
					boundConstructor = candidate;
				}
			}
			return boundConstructor;
		}

		static Bindable of(TypeElement type, MetadataGenerationEnvironment env) {
			List<ExecutableElement> constructors = ElementFilter.constructorsIn(type.getEnclosedElements());
			List<ExecutableElement> boundConstructors = getBoundConstructors(type, env, constructors);
			return new Bindable(type, constructors, boundConstructors);
		}

		private static List<ExecutableElement> getBoundConstructors(TypeElement type, MetadataGenerationEnvironment env,
				List<ExecutableElement> constructors) {
			ExecutableElement bindConstructor = deduceBindConstructor(type, constructors, env);
			if (bindConstructor != null) {
				return Collections.singletonList(bindConstructor);
			}
			return constructors.stream().filter(env::hasConstructorBindingAnnotation).toList();
		}

		private static ExecutableElement deduceBindConstructor(TypeElement type, List<ExecutableElement> constructors,
				MetadataGenerationEnvironment env) {
			if (constructors.size() == 1) {
				ExecutableElement candidate = constructors.get(0);
				if (!candidate.getParameters().isEmpty() && !env.hasAutowiredAnnotation(candidate)) {
					if (type.getNestingKind() == NestingKind.MEMBER
							&& candidate.getModifiers().contains(Modifier.PRIVATE)) {
						return null;
					}
					return candidate;
				}
			}
			return null;
		}

	}

}
