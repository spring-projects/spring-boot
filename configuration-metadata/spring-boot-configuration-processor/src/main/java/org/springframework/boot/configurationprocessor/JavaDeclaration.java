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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;

/**
 * {@link Declaration} backed by an {@link Element}.
 *
 * @param <E> the type of the element
 * @author Areg Iazychian
 */
abstract class JavaDeclaration<E extends Element> implements Declaration {

	private static final String DEPRECATED_ANNOTATION = "java.lang.Deprecated";

	private final JavaProcessingContext context;

	private final E element;

	JavaDeclaration(JavaProcessingContext context, E element) {
		this.context = context;
		this.element = element;
	}

	final JavaProcessingContext getContext() {
		return this.context;
	}

	final E getElement() {
		return this.element;
	}

	@Override
	public String getName() {
		return this.element.getSimpleName().toString();
	}

	@Override
	public TypeDeclaration getEnclosingType() {
		return (this.element.getEnclosingElement() instanceof TypeElement enclosingElement)
				? new JavaTypeDeclaration(this.context, enclosingElement) : null;
	}

	@Override
	public List<? extends AnnotationReference> getAnnotations() {
		return this.element.getAnnotationMirrors()
			.stream()
			.map((annotation) -> new JavaAnnotationReference(this.context, annotation))
			.toList();
	}

	@Override
	public boolean isDeprecated() {
		for (AnnotationMirror annotation : this.element.getAnnotationMirrors()) {
			if (DEPRECATED_ANNOTATION.equals(annotation.getAnnotationType().toString())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getDocComment() {
		return this.context.getTypeUtils().getJavaDoc(this.element);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.element.equals(((JavaDeclaration<?>) obj).element);
	}

	@Override
	public int hashCode() {
		return this.element.hashCode();
	}

	@Override
	public String toString() {
		return this.element.toString();
	}

}
