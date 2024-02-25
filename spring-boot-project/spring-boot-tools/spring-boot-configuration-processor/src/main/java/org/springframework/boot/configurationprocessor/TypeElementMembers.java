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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Provides access to relevant {@link TypeElement} members.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class TypeElementMembers {

	private static final String OBJECT_CLASS_NAME = Object.class.getName();

	private static final String RECORD_CLASS_NAME = "java.lang.Record";

	private final MetadataGenerationEnvironment env;

	private final TypeElement targetType;

	private final boolean isRecord;

	private final Map<String, VariableElement> fields = new LinkedHashMap<>();

	private final Map<String, List<ExecutableElement>> publicGetters = new LinkedHashMap<>();

	private final Map<String, List<ExecutableElement>> publicSetters = new LinkedHashMap<>();

	/**
     * Generates the members of a given TypeElement.
     * 
     * @param env The MetadataGenerationEnvironment used for generating metadata.
     * @param targetType The TypeElement for which the members need to be generated.
     */
    TypeElementMembers(MetadataGenerationEnvironment env, TypeElement targetType) {
		this.env = env;
		this.targetType = targetType;
		this.isRecord = RECORD_CLASS_NAME.equals(targetType.getSuperclass().toString());
		process(targetType);
	}

	/**
     * Processes the given TypeElement by iterating over its enclosed elements.
     * For each enclosed field, the method calls the processField() method.
     * For each enclosed method, the method calls the processMethod() method.
     * 
     * After processing the enclosed elements, the method checks if the TypeElement has a superclass.
     * If it does, and the superclass is not java.lang.Object or java.lang.Record, the method recursively
     * calls itself with the superclass as the argument.
     * 
     * @param element The TypeElement to be processed.
     */
    private void process(TypeElement element) {
		for (VariableElement field : ElementFilter.fieldsIn(element.getEnclosedElements())) {
			processField(field);
		}
		for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
			processMethod(method);
		}
		Element superType = this.env.getTypeUtils().asElement(element.getSuperclass());
		if (superType instanceof TypeElement && !OBJECT_CLASS_NAME.equals(superType.toString())
				&& !RECORD_CLASS_NAME.equals(superType.toString())) {
			process((TypeElement) superType);
		}
	}

	/**
     * Processes the given ExecutableElement method.
     * 
     * @param method the ExecutableElement method to process
     */
    private void processMethod(ExecutableElement method) {
		if (isPublic(method)) {
			String name = method.getSimpleName().toString();
			if (isGetter(method)) {
				String propertyName = getAccessorName(name);
				List<ExecutableElement> matchingGetters = this.publicGetters.computeIfAbsent(propertyName,
						(k) -> new ArrayList<>());
				TypeMirror returnType = method.getReturnType();
				if (getMatchingGetter(matchingGetters, returnType) == null) {
					matchingGetters.add(method);
				}
			}
			else if (isSetter(method)) {
				String propertyName = getAccessorName(name);
				List<ExecutableElement> matchingSetters = this.publicSetters.computeIfAbsent(propertyName,
						(k) -> new ArrayList<>());
				TypeMirror paramType = method.getParameters().get(0).asType();
				if (getMatchingSetter(matchingSetters, paramType) == null) {
					matchingSetters.add(method);
				}
			}
		}
	}

	/**
     * Checks if the given method is public.
     *
     * @param method the ExecutableElement representing the method to be checked
     * @return true if the method is public and not abstract or static, false otherwise
     */
    private boolean isPublic(ExecutableElement method) {
		Set<Modifier> modifiers = method.getModifiers();
		return modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.ABSTRACT)
				&& !modifiers.contains(Modifier.STATIC);
	}

	/**
     * Returns the matching getter method from the given list of candidates for the specified type.
     * 
     * @param candidates the list of candidate getter methods
     * @param type the type to match against
     * @return the matching getter method, or null if no match is found
     */
    ExecutableElement getMatchingGetter(List<ExecutableElement> candidates, TypeMirror type) {
		return getMatchingAccessor(candidates, type, ExecutableElement::getReturnType);
	}

	/**
     * Returns the matching setter method from the given list of candidates for the specified type.
     * 
     * @param candidates the list of setter methods to search from
     * @param type the type of the parameter that the setter method should accept
     * @return the matching setter method, or null if no match is found
     */
    private ExecutableElement getMatchingSetter(List<ExecutableElement> candidates, TypeMirror type) {
		return getMatchingAccessor(candidates, type, (candidate) -> candidate.getParameters().get(0).asType());
	}

	/**
     * Returns the matching accessor from the given list of candidates based on the provided type.
     * 
     * @param candidates The list of candidate accessor elements to search from.
     * @param type The type to match against.
     * @param typeExtractor The function to extract the type from an accessor element.
     * @return The matching accessor element, or null if no match is found.
     */
    private ExecutableElement getMatchingAccessor(List<ExecutableElement> candidates, TypeMirror type,
			Function<ExecutableElement, TypeMirror> typeExtractor) {
		for (ExecutableElement candidate : candidates) {
			TypeMirror candidateType = typeExtractor.apply(candidate);
			if (this.env.getTypeUtils().isSameType(candidateType, type)) {
				return candidate;
			}
		}
		return null;
	}

	/**
     * Checks if the given method is a getter method.
     *
     * @param method the method to check
     * @return true if the method is a getter method, false otherwise
     */
    private boolean isGetter(ExecutableElement method) {
		boolean hasParameters = !method.getParameters().isEmpty();
		boolean returnsVoid = TypeKind.VOID == method.getReturnType().getKind();
		if (hasParameters || returnsVoid) {
			return false;
		}
		String name = method.getSimpleName().toString();
		if (this.isRecord && this.fields.containsKey(name)) {
			return true;
		}
		return (name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2);
	}

	/**
     * Checks if the given method is a setter.
     * 
     * @param method the ExecutableElement representing the method to be checked
     * @return true if the method is a setter, false otherwise
     */
    private boolean isSetter(ExecutableElement method) {
		if (this.isRecord) {
			return false;
		}
		final String name = method.getSimpleName().toString();
		return (name.startsWith("set") && name.length() > 3 && method.getParameters().size() == 1
				&& isSetterReturnType(method));
	}

	/**
     * Checks if the given method is a setter method by examining its return type.
     * 
     * @param method the ExecutableElement representing the method to be checked
     * @return true if the method is a setter method, false otherwise
     */
    private boolean isSetterReturnType(ExecutableElement method) {
		TypeMirror returnType = method.getReturnType();
		if (TypeKind.VOID == returnType.getKind()) {
			return true;
		}
		if (TypeKind.DECLARED == returnType.getKind()
				&& this.env.getTypeUtils().isSameType(method.getEnclosingElement().asType(), returnType)) {
			return true;
		}
		if (TypeKind.TYPEVAR == returnType.getKind()) {
			String resolvedType = this.env.getTypeUtils().getType(this.targetType, returnType);
			return (resolvedType != null
					&& resolvedType.equals(this.env.getTypeUtils().getQualifiedName(this.targetType)));
		}
		return false;
	}

	/**
     * Returns the accessor name for a given method name.
     * 
     * @param methodName the method name
     * @return the accessor name
     * @throws IllegalStateException if the methodName does not start with 'is', 'get' or 'set'
     */
    private String getAccessorName(String methodName) {
		if (this.isRecord && this.fields.containsKey(methodName)) {
			return methodName;
		}
		if (methodName.startsWith("is")) {
			return lowerCaseFirstCharacter(methodName.substring(2));
		}
		if (methodName.startsWith("get") || methodName.startsWith("set")) {
			return lowerCaseFirstCharacter(methodName.substring(3));
		}
		throw new IllegalStateException("methodName must start with 'is', 'get' or 'set', was '" + methodName + "'");
	}

	/**
     * Converts the first character of the given string to lowercase.
     * 
     * @param string the string to convert
     * @return the string with the first character converted to lowercase
     */
    private String lowerCaseFirstCharacter(String string) {
		return Character.toLowerCase(string.charAt(0)) + string.substring(1);
	}

	/**
     * Processes a field and adds it to the fields map.
     * 
     * @param field the VariableElement representing the field to be processed
     */
    private void processField(VariableElement field) {
		String name = field.getSimpleName().toString();
		this.fields.putIfAbsent(name, field);
	}

	/**
     * Returns an unmodifiable map of fields in the TypeElementMembers class.
     *
     * @return an unmodifiable map of fields
     */
    Map<String, VariableElement> getFields() {
		return Collections.unmodifiableMap(this.fields);
	}

	/**
     * Returns an unmodifiable map of public getters.
     *
     * @return an unmodifiable map of public getters
     */
    Map<String, List<ExecutableElement>> getPublicGetters() {
		return Collections.unmodifiableMap(this.publicGetters);
	}

	/**
     * Returns the public getter method for the specified name and type.
     * 
     * @param name The name of the property.
     * @param type The type of the property.
     * @return The public getter method for the specified name and type, or null if not found.
     */
    ExecutableElement getPublicGetter(String name, TypeMirror type) {
		List<ExecutableElement> candidates = this.publicGetters.get(name);
		return getPublicAccessor(candidates, type, (specificType) -> getMatchingGetter(candidates, specificType));
	}

	/**
     * Returns the public setter method for the specified name and type.
     * 
     * @param name The name of the property.
     * @param type The type of the property.
     * @return The public setter method for the specified name and type, or null if not found.
     */
    ExecutableElement getPublicSetter(String name, TypeMirror type) {
		List<ExecutableElement> candidates = this.publicSetters.get(name);
		return getPublicAccessor(candidates, type, (specificType) -> getMatchingSetter(candidates, specificType));
	}

	/**
     * Returns the public accessor method from the given list of candidates for the specified type.
     * 
     * @param candidates                 the list of candidate accessor methods
     * @param type                       the type for which the accessor method is required
     * @param matchingAccessorExtractor  the function to extract the matching accessor method for the type
     * @return                           the public accessor method, or null if not found
     */
    private ExecutableElement getPublicAccessor(List<ExecutableElement> candidates, TypeMirror type,
			Function<TypeMirror, ExecutableElement> matchingAccessorExtractor) {
		if (candidates != null) {
			ExecutableElement matching = matchingAccessorExtractor.apply(type);
			if (matching != null) {
				return matching;
			}
			TypeMirror alternative = this.env.getTypeUtils().getWrapperOrPrimitiveFor(type);
			if (alternative != null) {
				return matchingAccessorExtractor.apply(alternative);
			}
		}
		return null;
	}

}
