/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

/**
 * Provides access to relevant {@link TypeElement} members.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
class TypeElementMembers {

	private static final String OBJECT_CLASS_NAME = Object.class.getName();

	private final ProcessingEnvironment env;

	private final Map<String, VariableElement> fields = new LinkedHashMap<String, VariableElement>();

	private final Map<String, ExecutableElement> publicGetters = new LinkedHashMap<String, ExecutableElement>();

	private final Map<String, ExecutableElement> publicSetters = new LinkedHashMap<String, ExecutableElement>();

	public TypeElementMembers(ProcessingEnvironment env, TypeElement element) {
		this.env = env;
		process(element);
	}

	private void process(TypeElement element) {
		for (ExecutableElement method : ElementFilter.methodsIn(element
				.getEnclosedElements())) {
			processMethod(method);
		}
		for (VariableElement field : ElementFilter
				.fieldsIn(element.getEnclosedElements())) {
			processField(field);
		}
		Element superType = this.env.getTypeUtils().asElement(element.getSuperclass());
		if (superType != null && superType instanceof TypeElement
				&& !OBJECT_CLASS_NAME.equals(superType.toString())) {
			process((TypeElement) superType);
		}
	}

	private void processMethod(ExecutableElement method) {
		if (method.getModifiers().contains(Modifier.PUBLIC)) {
			String name = method.getSimpleName().toString();
			if (isGetter(method) && !this.publicGetters.containsKey(name)) {
				this.publicGetters.put(getAccessorName(name), method);
			}
			else if (isSetter(method) && !this.publicSetters.containsKey(name)) {
				this.publicSetters.put(getAccessorName(name), method);
			}
		}
	}

	private boolean isGetter(ExecutableElement method) {
		String name = method.getSimpleName().toString();
		return (name.startsWith("get") || name.startsWith("is"))
				&& method.getParameters().isEmpty()
				&& (TypeKind.VOID != method.getReturnType().getKind());
	}

	private boolean isSetter(ExecutableElement method) {
		final String name = method.getSimpleName().toString();
		return name.startsWith("set") && method.getParameters().size() == 1
				&& (isSetterReturnType(method));
	}

	private boolean isSetterReturnType(ExecutableElement method) {
		return (TypeKind.VOID == method.getReturnType().getKind() || this.env
				.getTypeUtils().isSameType(method.getEnclosingElement().asType(),
						method.getReturnType()));
	}

	private String getAccessorName(String methodName) {
		String name = methodName.startsWith("is") ? methodName.substring(2) : methodName
				.substring(3);
		name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		return name;
	}

	private void processField(VariableElement field) {
		String name = field.getSimpleName().toString();
		if (!this.fields.containsKey(name)) {
			this.fields.put(name, field);
		}
	}

	public Map<String, VariableElement> getFields() {
		return Collections.unmodifiableMap(this.fields);
	}

	public Map<String, ExecutableElement> getPublicGetters() {
		return Collections.unmodifiableMap(this.publicGetters);
	}

	public Map<String, ExecutableElement> getPublicSetters() {
		return Collections.unmodifiableMap(this.publicSetters);
	}

}
