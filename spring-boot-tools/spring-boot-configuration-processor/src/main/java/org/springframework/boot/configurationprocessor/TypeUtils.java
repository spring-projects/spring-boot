/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Type Utilities.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
class TypeUtils {

	private static final Map<TypeKind, Class<?>> PRIMITIVE_WRAPPERS;

	static {
		Map<TypeKind, Class<?>> wrappers = new HashMap<TypeKind, Class<?>>();
		wrappers.put(TypeKind.BOOLEAN, Boolean.class);
		wrappers.put(TypeKind.BYTE, Byte.class);
		wrappers.put(TypeKind.CHAR, Character.class);
		wrappers.put(TypeKind.DOUBLE, Double.class);
		wrappers.put(TypeKind.FLOAT, Float.class);
		wrappers.put(TypeKind.INT, Integer.class);
		wrappers.put(TypeKind.LONG, Long.class);
		wrappers.put(TypeKind.SHORT, Short.class);
		PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(wrappers);
	}

	private static final Map<String, TypeKind> WRAPPER_TO_PRIMITIVE;

	static {
		Map<String, TypeKind> primitives = new HashMap<String, TypeKind>();
		for (Map.Entry<TypeKind, Class<?>> entry : PRIMITIVE_WRAPPERS.entrySet()) {
			primitives.put(entry.getValue().getName(), entry.getKey());
		}
		WRAPPER_TO_PRIMITIVE = primitives;
	}

	private final ProcessingEnvironment env;

	private final TypeMirror collectionType;

	private final TypeMirror mapType;

	TypeUtils(ProcessingEnvironment env) {
		this.env = env;
		Types types = env.getTypeUtils();
		this.collectionType = getDeclaredType(types, Collection.class, 1);
		this.mapType = getDeclaredType(types, Map.class, 2);
	}

	private TypeMirror getDeclaredType(Types types, Class<?> typeClass,
			int numberOfTypeArgs) {
		TypeMirror[] typeArgs = new TypeMirror[numberOfTypeArgs];
		for (int i = 0; i < typeArgs.length; i++) {
			typeArgs[i] = types.getWildcardType(null, null);
		}
		TypeElement typeElement = this.env.getElementUtils()
				.getTypeElement(typeClass.getName());
		try {
			return types.getDeclaredType(typeElement, typeArgs);
		}
		catch (IllegalArgumentException ex) {
			// Try again without generics for older Java versions
			return types.getDeclaredType(typeElement);
		}
	}

	public String getType(Element element) {
		return getType(element == null ? null : element.asType());
	}

	public String getType(TypeMirror type) {
		if (type == null) {
			return null;
		}
		Class<?> wrapper = getWrapperFor(type);
		if (wrapper != null) {
			return wrapper.getName();
		}
		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			Element enclosingElement = declaredType.asElement().getEnclosingElement();
			if (enclosingElement != null && enclosingElement instanceof TypeElement) {
				return getType(enclosingElement) + "$"
						+ declaredType.asElement().getSimpleName().toString();
			}
		}
		return type.toString();
	}

	public boolean isCollectionOrMap(TypeMirror type) {
		return this.env.getTypeUtils().isAssignable(type, this.collectionType)
				|| this.env.getTypeUtils().isAssignable(type, this.mapType);
	}

	public boolean isEnclosedIn(Element candidate, TypeElement element) {
		if (candidate == null || element == null) {
			return false;
		}
		if (candidate.equals(element)) {
			return true;
		}
		return isEnclosedIn(candidate.getEnclosingElement(), element);
	}

	public String getJavaDoc(Element element) {
		String javadoc = (element == null ? null
				: this.env.getElementUtils().getDocComment(element));
		if (javadoc != null) {
			javadoc = javadoc.trim();
		}
		return ("".equals(javadoc) ? null : javadoc);
	}

	public TypeMirror getWrapperOrPrimitiveFor(TypeMirror typeMirror) {
		Class<?> candidate = getWrapperFor(typeMirror);
		if (candidate != null) {
			return this.env.getElementUtils().getTypeElement(candidate.getName())
					.asType();
		}
		TypeKind primitiveKind = getPrimitiveFor(typeMirror);
		if (primitiveKind != null) {
			return this.env.getTypeUtils().getPrimitiveType(primitiveKind);
		}
		return null;
	}

	private Class<?> getWrapperFor(TypeMirror type) {
		return PRIMITIVE_WRAPPERS.get(type.getKind());
	}

	private TypeKind getPrimitiveFor(TypeMirror type) {
		return WRAPPER_TO_PRIMITIVE.get(type.toString());
	}

}
