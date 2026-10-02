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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;

/**
 * {@link AnnotationReference} backed by an {@link AnnotationMirror}.
 *
 * @author Areg Iazychian
 */
class JavaAnnotationReference implements AnnotationReference {

	private final JavaProcessingContext context;

	private final AnnotationMirror annotation;

	JavaAnnotationReference(JavaProcessingContext context, AnnotationMirror annotation) {
		this.context = context;
		this.annotation = annotation;
	}

	AnnotationMirror getAnnotation() {
		return this.annotation;
	}

	@Override
	public TypeDeclaration getType() {
		return new JavaTypeDeclaration(this.context, (TypeElement) this.annotation.getAnnotationType().asElement());
	}

	@Override
	public Map<String, Object> getValues() {
		Map<String, Object> values = new LinkedHashMap<>();
		this.annotation.getElementValues()
			.forEach((name, value) -> values.put(name.getSimpleName().toString(), getValue(value.getValue())));
		return values;
	}

	private Object getValue(Object value) {
		if (value instanceof List<?> list) {
			List<Object> values = new ArrayList<>();
			list.forEach((element) -> values.add(getValue(((AnnotationValue) element).getValue())));
			return values;
		}
		if (value instanceof VariableElement enumConstant) {
			return enumConstant.getSimpleName().toString();
		}
		return value;
	}

}
