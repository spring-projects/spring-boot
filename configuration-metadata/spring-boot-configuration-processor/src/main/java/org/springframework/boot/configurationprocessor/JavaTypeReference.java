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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;

/**
 * {@link TypeReference} backed by a {@link TypeMirror}.
 *
 * @author Areg Iazychian
 */
class JavaTypeReference implements TypeReference {

	private static final String NULLABLE_ANNOTATION = "org.jspecify.annotations.Nullable";

	private final JavaProcessingContext context;

	private final TypeMirror type;

	JavaTypeReference(JavaProcessingContext context, TypeMirror type) {
		this.context = context;
		this.type = type;
	}

	@Override
	public boolean isVoid() {
		return this.type.getKind() == TypeKind.VOID;
	}

	@Override
	public boolean isPrimitive() {
		return this.type.getKind().isPrimitive();
	}

	@Override
	public boolean isTypeVariable() {
		return this.type.getKind() == TypeKind.TYPEVAR;
	}

	@Override
	public boolean isNullable() {
		for (AnnotationMirror annotation : this.type.getAnnotationMirrors()) {
			if (NULLABLE_ANNOTATION.equals(annotation.getAnnotationType().toString())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCollectionOrMap() {
		return this.context.getTypeUtils().isCollectionOrMap(this.type);
	}

	@Override
	public boolean isSameType(TypeReference other) {
		return this.context.getTypeUtils().isSameType(this.type, ((JavaTypeReference) other).type);
	}

	@Override
	public TypeDeclaration getDeclaration() {
		Element element = this.context.getTypeUtils().asElement(this.type);
		return (element instanceof TypeElement typeElement) ? new JavaTypeDeclaration(this.context, typeElement) : null;
	}

	@Override
	public TypeReference getElementType() {
		TypeMirror elementType = this.context.getTypeUtils().extractElementType(this.type);
		return (elementType != null) ? new JavaTypeReference(this.context, elementType) : null;
	}

	@Override
	public String getName(TypeDeclaration declaringType) {
		return this.context.getTypeUtils().getType(((JavaTypeDeclaration) declaringType).getElement(), this.type);
	}

	@Override
	public String toString() {
		return this.type.toString();
	}

}
