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
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

/**
 * {@link TypeDeclaration} backed by a {@link TypeElement}.
 *
 * @author Areg Iazychian
 */
class JavaTypeDeclaration extends JavaDeclaration<TypeElement> implements TypeDeclaration {

	private static final String RECORD_CLASS_NAME = Record.class.getName();

	JavaTypeDeclaration(JavaProcessingContext context, TypeElement element) {
		super(context, element);
	}

	@Override
	public String getQualifiedName() {
		return getContext().getTypeUtils().getQualifiedName(getElement());
	}

	@Override
	public TypeDeclaration getSuperclass() {
		Element superType = getContext().getTypeUtils().asElement(getElement().getSuperclass());
		return (superType instanceof TypeElement typeElement) ? new JavaTypeDeclaration(getContext(), typeElement)
				: null;
	}

	@Override
	public TypeReference asType() {
		return new JavaTypeReference(getContext(), getElement().asType());
	}

	@Override
	public boolean isEnum() {
		return getElement().getKind() == ElementKind.ENUM;
	}

	@Override
	public boolean isRecord() {
		return RECORD_CLASS_NAME.equals(getElement().getSuperclass().toString());
	}

	@Override
	public boolean hasSource() {
		return getContext().getFieldValuesParser().hasSourceTree(getElement());
	}

	@Override
	public List<? extends MethodDeclaration> getConstructors() {
		return ElementFilter.constructorsIn(getElement().getEnclosedElements())
			.stream()
			.map((constructor) -> new JavaMethodDeclaration(getContext(), constructor))
			.toList();
	}

	@Override
	public List<? extends MethodDeclaration> getMethods() {
		return ElementFilter.methodsIn(getElement().getEnclosedElements())
			.stream()
			.map((method) -> new JavaMethodDeclaration(getContext(), method))
			.toList();
	}

	@Override
	public List<? extends VariableDeclaration> getFields() {
		return ElementFilter.fieldsIn(getElement().getEnclosedElements())
			.stream()
			.map((field) -> new JavaVariableDeclaration(getContext(), field))
			.toList();
	}

	@Override
	public List<? extends VariableDeclaration> getRecordComponents() {
		return ElementFilter.recordComponentsIn(getElement().getEnclosedElements())
			.stream()
			.map((recordComponent) -> new JavaVariableDeclaration(getContext(), recordComponent))
			.toList();
	}

	@Override
	public Map<String, Object> getFieldValues() {
		try {
			return getContext().getFieldValuesParser().getFieldValues(getElement());
		}
		catch (Exception ex) {
			return Collections.emptyMap();
		}
	}

}
