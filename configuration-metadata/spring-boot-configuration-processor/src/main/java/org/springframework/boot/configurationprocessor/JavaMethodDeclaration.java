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

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

/**
 * {@link MethodDeclaration} backed by an {@link ExecutableElement}.
 *
 * @author Areg Iazychian
 */
class JavaMethodDeclaration extends JavaDeclaration<ExecutableElement> implements MethodDeclaration {

	JavaMethodDeclaration(JavaProcessingContext context, ExecutableElement element) {
		super(context, element);
	}

	@Override
	public List<? extends VariableDeclaration> getParameters() {
		return getElement().getParameters()
			.stream()
			.map((parameter) -> new JavaVariableDeclaration(getContext(), parameter))
			.toList();
	}

	@Override
	public TypeReference getReturnType() {
		return new JavaTypeReference(getContext(), getElement().getReturnType());
	}

	@Override
	public String getSignature() {
		return getElement().toString();
	}

	@Override
	public boolean isPublic() {
		return getElement().getModifiers().contains(Modifier.PUBLIC);
	}

	@Override
	public boolean isPrivate() {
		return getElement().getModifiers().contains(Modifier.PRIVATE);
	}

	@Override
	public boolean isAbstract() {
		return getElement().getModifiers().contains(Modifier.ABSTRACT);
	}

	@Override
	public boolean isStatic() {
		return getElement().getModifiers().contains(Modifier.STATIC);
	}

}
