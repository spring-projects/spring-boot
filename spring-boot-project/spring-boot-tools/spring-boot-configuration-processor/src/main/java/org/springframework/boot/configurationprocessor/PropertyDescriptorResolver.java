/*
 * Copyright 2012-2023 the original author or authors.
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Resolve {@link PropertyDescriptor} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class PropertyDescriptorResolver {

	private final MetadataGenerationEnvironment environment;

	/**
     * Constructs a new PropertyDescriptorResolver with the specified MetadataGenerationEnvironment.
     * 
     * @param environment the MetadataGenerationEnvironment to be used by the resolver
     */
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
	Stream<PropertyDescriptor<?>> resolve(TypeElement type, ExecutableElement factoryMethod) {
		TypeElementMembers members = new TypeElementMembers(this.environment, type);
		if (factoryMethod != null) {
			return resolveJavaBeanProperties(type, factoryMethod, members);
		}
		return resolve(ConfigurationPropertiesTypeElement.of(type, this.environment), members);
	}

	/**
     * Resolves the property descriptors for the given configuration properties type element and type element members.
     * 
     * @param type the configuration properties type element
     * @param members the type element members
     * @return a stream of property descriptors
     */
    private Stream<PropertyDescriptor<?>> resolve(ConfigurationPropertiesTypeElement type, TypeElementMembers members) {
		if (type.isConstructorBindingEnabled()) {
			ExecutableElement constructor = type.getBindConstructor();
			if (constructor != null) {
				return resolveConstructorProperties(type.getType(), members, constructor);
			}
			return Stream.empty();
		}
		return resolveJavaBeanProperties(type.getType(), null, members);
	}

	/**
     * Resolves the constructor properties for a given type and constructor.
     * 
     * @param type        the TypeElement representing the type
     * @param members     the TypeElementMembers containing the members of the type
     * @param constructor the ExecutableElement representing the constructor
     * @return a Stream of PropertyDescriptor objects representing the resolved constructor properties
     */
    Stream<PropertyDescriptor<?>> resolveConstructorProperties(TypeElement type, TypeElementMembers members,
			ExecutableElement constructor) {
		Map<String, PropertyDescriptor<?>> candidates = new LinkedHashMap<>();
		constructor.getParameters().forEach((parameter) -> {
			String name = getParameterName(parameter);
			TypeMirror propertyType = parameter.asType();
			ExecutableElement getter = members.getPublicGetter(name, propertyType);
			ExecutableElement setter = members.getPublicSetter(name, propertyType);
			VariableElement field = members.getFields().get(name);
			register(candidates, new ConstructorParameterPropertyDescriptor(type, null, parameter, name, propertyType,
					field, getter, setter));
		});
		return candidates.values().stream();
	}

	/**
     * Returns the name of the parameter.
     * 
     * @param parameter the VariableElement representing the parameter
     * @return the name of the parameter
     */
    private String getParameterName(VariableElement parameter) {
		AnnotationMirror nameAnnotation = this.environment.getNameAnnotation(parameter);
		if (nameAnnotation != null) {
			return this.environment.getAnnotationElementStringValue(nameAnnotation, "value");
		}
		return parameter.getSimpleName().toString();
	}

	/**
     * Resolves the Java bean properties for a given type element, factory method, and type element members.
     * 
     * @param type the type element representing the class or interface
     * @param factoryMethod the executable element representing the factory method
     * @param members the type element members containing the fields, getters, and setters
     * @return a stream of property descriptors for the resolved Java bean properties
     */
    Stream<PropertyDescriptor<?>> resolveJavaBeanProperties(TypeElement type, ExecutableElement factoryMethod,
			TypeElementMembers members) {
		// First check if we have regular java bean properties there
		Map<String, PropertyDescriptor<?>> candidates = new LinkedHashMap<>();
		members.getPublicGetters().forEach((name, getters) -> {
			VariableElement field = members.getFields().get(name);
			ExecutableElement getter = findMatchingGetter(members, getters, field);
			TypeMirror propertyType = getter.getReturnType();
			register(candidates, new JavaBeanPropertyDescriptor(type, factoryMethod, getter, name, propertyType, field,
					members.getPublicSetter(name, propertyType)));
		});
		// Then check for Lombok ones
		members.getFields().forEach((name, field) -> {
			TypeMirror propertyType = field.asType();
			ExecutableElement getter = members.getPublicGetter(name, propertyType);
			ExecutableElement setter = members.getPublicSetter(name, propertyType);
			register(candidates,
					new LombokPropertyDescriptor(type, factoryMethod, field, name, propertyType, getter, setter));
		});
		return candidates.values().stream();
	}

	/**
     * Finds the matching getter method for a given field.
     * 
     * @param members    the TypeElementMembers object containing the members of the enclosing type
     * @param candidates the list of candidate getter methods
     * @param field      the field for which the getter method is being searched
     * @return the matching getter method, or the first candidate if no match is found
     */
    private ExecutableElement findMatchingGetter(TypeElementMembers members, List<ExecutableElement> candidates,
			VariableElement field) {
		if (candidates.size() > 1 && field != null) {
			return members.getMatchingGetter(candidates, field.asType());
		}
		return candidates.get(0);
	}

	/**
     * Registers a property descriptor in the given map of candidates.
     * 
     * @param candidates the map of candidates to register the descriptor in
     * @param descriptor the property descriptor to be registered
     */
    private void register(Map<String, PropertyDescriptor<?>> candidates, PropertyDescriptor<?> descriptor) {
		if (!candidates.containsKey(descriptor.getName()) && isCandidate(descriptor)) {
			candidates.put(descriptor.getName(), descriptor);
		}
	}

	/**
     * Checks if the given PropertyDescriptor is a candidate based on the current environment.
     * 
     * @param descriptor the PropertyDescriptor to check
     * @return true if the descriptor is a candidate, false otherwise
     */
    private boolean isCandidate(PropertyDescriptor<?> descriptor) {
		return descriptor.isProperty(this.environment) || descriptor.isNested(this.environment);
	}

	/**
	 * Wrapper around a {@link TypeElement} that could be bound.
	 */
	private static class ConfigurationPropertiesTypeElement {

		private final TypeElement type;

		private final List<ExecutableElement> constructors;

		private final List<ExecutableElement> boundConstructors;

		/**
         * Creates a new instance of ConfigurationPropertiesTypeElement.
         * 
         * @param type The TypeElement representing the configuration properties type.
         * @param constructors The list of constructors for the configuration properties type.
         * @param boundConstructors The list of bound constructors for the configuration properties type.
         */
        ConfigurationPropertiesTypeElement(TypeElement type, List<ExecutableElement> constructors,
				List<ExecutableElement> boundConstructors) {
			this.type = type;
			this.constructors = constructors;
			this.boundConstructors = boundConstructors;
		}

		/**
         * Returns the type of the ConfigurationPropertiesTypeElement.
         *
         * @return the type of the ConfigurationPropertiesTypeElement
         */
        TypeElement getType() {
			return this.type;
		}

		/**
         * Returns a boolean value indicating whether constructor binding is enabled.
         * 
         * @return {@code true} if constructor binding is enabled, {@code false} otherwise
         */
        boolean isConstructorBindingEnabled() {
			return !this.boundConstructors.isEmpty();
		}

		/**
         * Returns the bind constructor for this ConfigurationPropertiesTypeElement.
         * If the boundConstructors list is empty, it will try to find and return the bound constructor.
         * If there is only one bound constructor in the list, it will return that constructor.
         * If there are multiple bound constructors in the list, it will return null.
         *
         * @return the bind constructor for this ConfigurationPropertiesTypeElement, or null if there are multiple bound constructors
         */
        ExecutableElement getBindConstructor() {
			if (this.boundConstructors.isEmpty()) {
				return findBoundConstructor();
			}
			if (this.boundConstructors.size() == 1) {
				return this.boundConstructors.get(0);
			}
			return null;
		}

		/**
         * Finds the bound constructor in the ConfigurationPropertiesTypeElement class.
         * 
         * @return The bound constructor if found, null otherwise.
         */
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

		/**
         * Creates a new ConfigurationPropertiesTypeElement object based on the given TypeElement and MetadataGenerationEnvironment.
         * 
         * @param type the TypeElement representing the configuration properties type
         * @param env the MetadataGenerationEnvironment used for metadata generation
         * @return a new ConfigurationPropertiesTypeElement object
         */
        static ConfigurationPropertiesTypeElement of(TypeElement type, MetadataGenerationEnvironment env) {
			List<ExecutableElement> constructors = ElementFilter.constructorsIn(type.getEnclosedElements());
			List<ExecutableElement> boundConstructors = getBoundConstructors(type, env, constructors);
			return new ConfigurationPropertiesTypeElement(type, constructors, boundConstructors);
		}

		/**
         * Retrieves the bound constructors for the given type element.
         * 
         * @param type the type element for which to retrieve the bound constructors
         * @param env the metadata generation environment
         * @param constructors the list of constructors to filter
         * @return the list of bound constructors
         */
        private static List<ExecutableElement> getBoundConstructors(TypeElement type, MetadataGenerationEnvironment env,
				List<ExecutableElement> constructors) {
			ExecutableElement bindConstructor = deduceBindConstructor(type, constructors, env);
			if (bindConstructor != null) {
				return Collections.singletonList(bindConstructor);
			}
			return constructors.stream().filter(env::hasConstructorBindingAnnotation).toList();
		}

		/**
         * Deduces the bind constructor for the given type element and list of constructors.
         * 
         * @param type the type element for which to deduce the bind constructor
         * @param constructors the list of constructors to consider
         * @param env the metadata generation environment
         * @return the bind constructor, or null if none is found
         */
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
