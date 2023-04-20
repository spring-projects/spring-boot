/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Utilities for dealing with {@link Element} classes.
 *
 * @author Phillip Webb
 */
final class Elements {

	private Elements() {
	}

	static String getQualifiedName(Element element) {
		if (element != null) {
			TypeElement enclosingElement = getEnclosingTypeElement(element.asType());
			if (enclosingElement != null) {
				return getQualifiedName(enclosingElement) + "$"
						+ ((DeclaredType) element.asType()).asElement().getSimpleName().toString();
			}
			if (element instanceof TypeElement typeElement) {
				return typeElement.getQualifiedName().toString();
			}
		}
		return null;
	}

	private static TypeElement getEnclosingTypeElement(TypeMirror type) {
		if (type instanceof DeclaredType declaredType) {
			Element enclosingElement = declaredType.asElement().getEnclosingElement();
			if (enclosingElement instanceof TypeElement typeElement) {
				return typeElement;
			}
		}
		return null;
	}

}
