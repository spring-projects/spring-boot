/*
 * Copyright 2012-present the original author or authors.
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
