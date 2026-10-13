/*
 * Copyright 2012-present the original author or authors.
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
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.boot.configurationprocessor.ConfigurationPropertiesSourceResolver.SourceMetadata;
import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

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
	 * specified {@link TypeDeclaration type} based on the specified
	 * {@link MethodDeclaration factory method}, if any.
	 * @param type the target type
	 * @param factoryMethod the method that triggered the metadata for that {@code type}
	 * or {@code null}
	 * @return the candidate properties for metadata generation
	 */
	Stream<PropertyDescriptor> resolve(TypeDeclaration type, MethodDeclaration factoryMethod) {
		TypeElementMembers members = new TypeElementMembers(type);
		if (factoryMethod != null) {
			return resolveJavaBeanProperties(type, members, factoryMethod);
		}
		return resolve(Bindable.of(type, this.environment), members);
	}

	private Stream<PropertyDescriptor> resolve(Bindable bindable, TypeElementMembers members) {
		if (bindable.isConstructorBindingEnabled()) {
			MethodDeclaration bindConstructor = bindable.getBindConstructor();
			return (bindConstructor != null)
					? resolveConstructorBoundProperties(bindable.getType(), members, bindConstructor) : Stream.empty();
		}
		return resolveJavaBeanProperties(bindable.getType(), members, null);
	}

	private Stream<PropertyDescriptor> resolveConstructorBoundProperties(TypeDeclaration declaringElement,
			TypeElementMembers members, MethodDeclaration bindConstructor) {
		Map<String, PropertyDescriptor> candidates = new LinkedHashMap<>();
		bindConstructor.getParameters().forEach((parameter) -> {
			PropertyDescriptor descriptor = extracted(declaringElement, members, parameter);
			register(candidates, descriptor);
		});
		return candidates.values().stream();
	}

	private PropertyDescriptor extracted(TypeDeclaration declaringElement, TypeElementMembers members,
			VariableDeclaration parameter) {
		String parameterName = parameter.getName();
		String name = getPropertyName(parameter, parameterName);
		TypeReference type = parameter.getType();
		MethodDeclaration getter = members.getPublicGetter(parameterName, type);
		MethodDeclaration setter = members.getPublicSetter(parameterName, type);
		VariableDeclaration field = members.getFields().get(parameterName);
		VariableDeclaration recordComponent = members.getRecordComponents().get(parameterName);
		SourceMetadata sourceMetadata = this.environment.resolveSourceMetadata(field, getter);
		PropertyDescriptor propertyDescriptor = (recordComponent != null)
				? new RecordParameterPropertyDescriptor(name, type, parameter, declaringElement, getter,
						recordComponent)
				: new ConstructorParameterPropertyDescriptor(name, type, parameter, declaringElement, getter, setter,
						field);
		return sourceMetadata.createPropertyDescriptor(name, propertyDescriptor);
	}

	private String getPropertyName(VariableDeclaration parameter, String fallback) {
		AnnotationReference nameAnnotation = this.environment.getNameAnnotation(parameter);
		if (nameAnnotation != null) {
			return this.environment.getAnnotationElementStringValue(nameAnnotation, "value");
		}
		return fallback;
	}

	private Stream<PropertyDescriptor> resolveJavaBeanProperties(TypeDeclaration declaringElement,
			TypeElementMembers members, MethodDeclaration factoryMethod) {
		// First check if we have regular java bean properties there
		Map<String, PropertyDescriptor> candidates = new LinkedHashMap<>();
		members.getPublicGetters().forEach((name, getters) -> {
			VariableDeclaration field = members.getFields().get(name);
			MethodDeclaration getter = findMatchingGetter(members, getters, field);
			TypeReference propertyType = getter.getReturnType();
			SourceMetadata sourceMetadata = this.environment.resolveSourceMetadata(field, getter);
			register(candidates,
					sourceMetadata.createPropertyDescriptor(getPropertyName(field, name),
							(propertyName) -> new JavaBeanPropertyDescriptor(propertyName, propertyType,
									declaringElement, getter, members.getPublicSetter(name, propertyType), field,
									factoryMethod)));
		});
		// Then check for Lombok ones
		members.getFields().forEach((name, field) -> {
			TypeReference propertyType = field.getType();
			MethodDeclaration getter = members.getPublicGetter(name, propertyType);
			MethodDeclaration setter = members.getPublicSetter(name, propertyType);
			SourceMetadata sourceMetadata = this.environment.resolveSourceMetadata(field, getter);
			register(candidates,
					sourceMetadata.createPropertyDescriptor(getPropertyName(field, name),
							(propertyName) -> new LombokPropertyDescriptor(propertyName, propertyType, declaringElement,
									getter, setter, field, factoryMethod)));
		});
		return candidates.values().stream();
	}

	private MethodDeclaration findMatchingGetter(TypeElementMembers members, List<MethodDeclaration> candidates,
			VariableDeclaration field) {
		if (candidates.size() > 1 && field != null) {
			return members.getMatchingGetter(candidates, field.getType());
		}
		return candidates.get(0);
	}

	private void register(Map<String, PropertyDescriptor> candidates, PropertyDescriptor descriptor) {
		if (!isCandidate(descriptor)) {
			return;
		}
		PropertyDescriptor existing = candidates.get(descriptor.getName());
		if (existing == null) {
			candidates.put(descriptor.getName(), descriptor);
		}
		else if (isDistinctDescriptor(existing, descriptor)) {
			this.environment.getContext()
				.error("Property name '%s' maps to distinct properties in type %s".formatted(descriptor.getName(),
						descriptor.getDeclaringElement().getQualifiedName()), null, null);
		}
	}

	private boolean isCandidate(PropertyDescriptor descriptor) {
		return descriptor.isProperty(this.environment) || descriptor.isNested(this.environment);
	}

	private boolean isDistinctDescriptor(PropertyDescriptor existing, PropertyDescriptor candidate) {
		return Objects.equals(existing.getClass(), candidate.getClass())
				&& !Objects.equals(existing.getSourceElement(), candidate.getSourceElement());
	}

	/**
	 * Wrapper around a {@link TypeDeclaration} that could be bound.
	 */
	private static class Bindable {

		private final TypeDeclaration type;

		private final List<MethodDeclaration> constructors;

		private final List<MethodDeclaration> boundConstructors;

		Bindable(TypeDeclaration type, List<MethodDeclaration> constructors,
				List<MethodDeclaration> boundConstructors) {
			this.type = type;
			this.constructors = constructors;
			this.boundConstructors = boundConstructors;
		}

		TypeDeclaration getType() {
			return this.type;
		}

		boolean isConstructorBindingEnabled() {
			return !this.boundConstructors.isEmpty();
		}

		MethodDeclaration getBindConstructor() {
			if (this.boundConstructors.isEmpty()) {
				return findBoundConstructor();
			}
			if (this.boundConstructors.size() == 1) {
				return this.boundConstructors.get(0);
			}
			return null;
		}

		private MethodDeclaration findBoundConstructor() {
			MethodDeclaration boundConstructor = null;
			for (MethodDeclaration candidate : this.constructors) {
				if (!candidate.getParameters().isEmpty()) {
					if (boundConstructor != null) {
						return null;
					}
					boundConstructor = candidate;
				}
			}
			return boundConstructor;
		}

		static Bindable of(TypeDeclaration type, MetadataGenerationEnvironment env) {
			List<MethodDeclaration> constructors = List.copyOf(type.getConstructors());
			List<MethodDeclaration> boundConstructors = getBoundConstructors(type, env, constructors);
			return new Bindable(type, constructors, boundConstructors);
		}

		private static List<MethodDeclaration> getBoundConstructors(TypeDeclaration type,
				MetadataGenerationEnvironment env, List<MethodDeclaration> constructors) {
			MethodDeclaration bindConstructor = deduceBindConstructor(type, constructors, env);
			if (bindConstructor != null) {
				return Collections.singletonList(bindConstructor);
			}
			return constructors.stream().filter(env::hasConstructorBindingAnnotation).toList();
		}

		private static MethodDeclaration deduceBindConstructor(TypeDeclaration type,
				List<MethodDeclaration> constructors, MetadataGenerationEnvironment env) {
			if (constructors.size() == 1) {
				MethodDeclaration candidate = constructors.get(0);
				if (!candidate.getParameters().isEmpty() && !env.hasAutowiredAnnotation(candidate)) {
					if (type.getEnclosingType() != null && candidate.isPrivate()) {
						return null;
					}
					return candidate;
				}
			}
			return null;
		}

	}

}
