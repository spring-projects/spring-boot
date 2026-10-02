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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

/**
 * Provides access to relevant {@link TypeDeclaration} members.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author Pavel Anisimov
 */
class TypeElementMembers {

	private static final String OBJECT_CLASS_NAME = Object.class.getName();

	private static final String RECORD_CLASS_NAME = Record.class.getName();

	private static final Set<String> WRAPPER_CLASS_NAMES = Set.of(Boolean.class.getName(), Byte.class.getName(),
			Character.class.getName(), Double.class.getName(), Float.class.getName(), Integer.class.getName(),
			Long.class.getName(), Short.class.getName());

	private final TypeDeclaration targetType;

	private final boolean isRecord;

	private final Map<String, VariableDeclaration> fields = new LinkedHashMap<>();

	private final Map<String, VariableDeclaration> recordComponents = new LinkedHashMap<>();

	private final Map<String, List<MethodDeclaration>> publicGetters = new LinkedHashMap<>();

	private final Map<String, List<MethodDeclaration>> publicSetters = new LinkedHashMap<>();

	TypeElementMembers(TypeDeclaration targetType) {
		this.targetType = targetType;
		this.isRecord = targetType.isRecord();
		process(targetType);
	}

	private void process(TypeDeclaration element) {
		for (VariableDeclaration field : element.getFields()) {
			processField(field);
		}
		for (VariableDeclaration recordComponent : element.getRecordComponents()) {
			processRecordComponent(recordComponent);
		}
		for (MethodDeclaration method : element.getMethods()) {
			processMethod(method);
		}
		TypeDeclaration superType = element.getSuperclass();
		if (superType != null && !OBJECT_CLASS_NAME.equals(superType.getQualifiedName())
				&& !RECORD_CLASS_NAME.equals(superType.getQualifiedName())) {
			process(superType);
		}
	}

	private void processMethod(MethodDeclaration method) {
		if (isPublic(method)) {
			String name = method.getName();
			if (isGetter(method)) {
				String propertyName = getAccessorName(name);
				List<MethodDeclaration> matchingGetters = this.publicGetters.computeIfAbsent(propertyName,
						(k) -> new ArrayList<>());
				TypeReference returnType = method.getReturnType();
				if (getMatchingGetter(matchingGetters, returnType) == null) {
					matchingGetters.add(method);
				}
			}
			else if (isSetter(method)) {
				String propertyName = getAccessorName(name);
				List<MethodDeclaration> matchingSetters = this.publicSetters.computeIfAbsent(propertyName,
						(k) -> new ArrayList<>());
				TypeReference paramType = method.getParameters().get(0).getType();
				if (getMatchingSetter(matchingSetters, paramType) == null) {
					matchingSetters.add(method);
				}
			}
		}
	}

	private boolean isPublic(MethodDeclaration method) {
		return method.isPublic() && !method.isAbstract() && !method.isStatic();
	}

	MethodDeclaration getMatchingGetter(List<MethodDeclaration> candidates, TypeReference type) {
		return getMatchingAccessor(candidates, MethodDeclaration::getReturnType, type::isSameType);
	}

	private MethodDeclaration getMatchingSetter(List<MethodDeclaration> candidates, TypeReference type) {
		return getMatchingAccessor(candidates, this::getSetterType, type::isSameType);
	}

	private TypeReference getSetterType(MethodDeclaration setter) {
		return setter.getParameters().get(0).getType();
	}

	private MethodDeclaration getMatchingAccessor(List<MethodDeclaration> candidates,
			Function<MethodDeclaration, TypeReference> typeExtractor, Predicate<TypeReference> typeFilter) {
		for (MethodDeclaration candidate : candidates) {
			if (typeFilter.test(typeExtractor.apply(candidate))) {
				return candidate;
			}
		}
		return null;
	}

	private boolean isGetter(MethodDeclaration method) {
		boolean hasParameters = !method.getParameters().isEmpty();
		boolean returnsVoid = method.getReturnType().isVoid();
		if (hasParameters || returnsVoid) {
			return false;
		}
		String name = method.getName();
		if (this.isRecord && this.fields.containsKey(name)) {
			return true;
		}
		return (name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2);
	}

	private boolean isSetter(MethodDeclaration method) {
		if (this.isRecord) {
			return false;
		}
		final String name = method.getName();
		return (name.startsWith("set") && name.length() > 3 && method.getParameters().size() == 1
				&& isSetterReturnType(method));
	}

	private boolean isSetterReturnType(MethodDeclaration method) {
		TypeReference returnType = method.getReturnType();
		if (returnType.isVoid()) {
			return true;
		}
		if (returnType.isTypeVariable()) {
			String resolvedType = returnType.getName(this.targetType);
			return (resolvedType != null && resolvedType.equals(this.targetType.getQualifiedName()));
		}
		return method.getEnclosingType().asType().isSameType(returnType);
	}

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

	private String lowerCaseFirstCharacter(String string) {
		return Character.toLowerCase(string.charAt(0)) + string.substring(1);
	}

	private void processField(VariableDeclaration field) {
		String name = field.getName();
		this.fields.putIfAbsent(name, field);
	}

	private void processRecordComponent(VariableDeclaration recordComponent) {
		String name = recordComponent.getName();
		this.recordComponents.putIfAbsent(name, recordComponent);
	}

	Map<String, VariableDeclaration> getFields() {
		return Collections.unmodifiableMap(this.fields);
	}

	Map<String, VariableDeclaration> getRecordComponents() {
		return Collections.unmodifiableMap(this.recordComponents);
	}

	Map<String, List<MethodDeclaration>> getPublicGetters() {
		return Collections.unmodifiableMap(this.publicGetters);
	}

	MethodDeclaration getPublicGetter(String name, TypeReference type) {
		return getPublicAccessor(this.publicGetters.get(name), MethodDeclaration::getReturnType, type);
	}

	MethodDeclaration getPublicSetter(String name, TypeReference type) {
		return getPublicAccessor(this.publicSetters.get(name), this::getSetterType, type);
	}

	private MethodDeclaration getPublicAccessor(List<MethodDeclaration> candidates,
			Function<MethodDeclaration, TypeReference> typeExtractor, TypeReference type) {
		if (candidates != null) {
			MethodDeclaration matching = getMatchingAccessor(candidates, typeExtractor, type::isSameType);
			if (matching != null) {
				return matching;
			}
			String wrapperName = getWrapperName(type);
			if (wrapperName != null) {
				return getMatchingAccessor(candidates, typeExtractor,
						(candidateType) -> candidateType.isPrimitive() != type.isPrimitive()
								&& wrapperName.equals(candidateType.getName(this.targetType)));
			}
		}
		return null;
	}

	/**
	 * Return the name of the wrapper class of the given {@code type} if it is a primitive
	 * type or one of their wrappers.
	 * @param type the type
	 * @return the name of the wrapper class or {@code null}
	 */
	private String getWrapperName(TypeReference type) {
		String name = (type.isPrimitive()) ? type.getName(this.targetType) : type.toString();
		return (WRAPPER_CLASS_NAMES.contains(name)) ? name : null;
	}

}
