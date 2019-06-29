/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;

/**
 * Type Utilities.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class TypeUtils {

	private static final Map<TypeKind, Class<?>> PRIMITIVE_WRAPPERS;

	static {
		Map<TypeKind, Class<?>> wrappers = new EnumMap<>(TypeKind.class);
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
		Map<String, TypeKind> primitives = new HashMap<>();
		PRIMITIVE_WRAPPERS.forEach((kind, wrapperClass) -> primitives.put(wrapperClass.getName(), kind));
		WRAPPER_TO_PRIMITIVE = primitives;
	}

	private final ProcessingEnvironment env;

	private final TypeExtractor typeExtractor;

	private final TypeMirror collectionType;

	private final TypeMirror mapType;

	TypeUtils(ProcessingEnvironment env) {
		this.env = env;
		Types types = env.getTypeUtils();
		this.typeExtractor = new TypeExtractor(types);
		this.collectionType = getDeclaredType(types, Collection.class, 1);
		this.mapType = getDeclaredType(types, Map.class, 2);
	}

	private TypeMirror getDeclaredType(Types types, Class<?> typeClass, int numberOfTypeArgs) {
		TypeMirror[] typeArgs = new TypeMirror[numberOfTypeArgs];
		Arrays.setAll(typeArgs, (i) -> types.getWildcardType(null, null));
		TypeElement typeElement = this.env.getElementUtils().getTypeElement(typeClass.getName());
		try {
			return types.getDeclaredType(typeElement, typeArgs);
		}
		catch (IllegalArgumentException ex) {
			// Try again without generics for older Java versions
			return types.getDeclaredType(typeElement);
		}
	}

	/**
	 * Return the qualified name of the specified element.
	 * @param element the element to handle
	 * @return the fully qualified name of the element, suitable for a call to
	 * {@link Class#forName(String)}
	 */
	public String getQualifiedName(Element element) {
		return this.typeExtractor.getQualifiedName(element);
	}

	/**
	 * Return the type of the specified {@link TypeMirror} including all its generic
	 * information.
	 * @param type the type to handle
	 * @return a representation of the type including all its generic information
	 */
	public String getType(TypeMirror type) {
		if (type == null) {
			return null;
		}
		return type.accept(this.typeExtractor, null);
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
		String javadoc = (element != null) ? this.env.getElementUtils().getDocComment(element) : null;
		if (javadoc != null) {
			javadoc = javadoc.replaceAll("[\r\n]+", "").trim();
		}
		return "".equals(javadoc) ? null : javadoc;
	}

	public TypeMirror getWrapperOrPrimitiveFor(TypeMirror typeMirror) {
		Class<?> candidate = getWrapperFor(typeMirror);
		if (candidate != null) {
			return this.env.getElementUtils().getTypeElement(candidate.getName()).asType();
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

	/**
	 * A visitor that extracts the fully qualified name of a type, including generic
	 * information.
	 */
	private static class TypeExtractor extends SimpleTypeVisitor8<String, Void> {

		private final Types types;

		TypeExtractor(Types types) {
			this.types = types;
		}

		@Override
		public String visitDeclared(DeclaredType type, Void none) {
			TypeElement enclosingElement = getEnclosingTypeElement(type);
			if (enclosingElement != null) {
				return getQualifiedName(enclosingElement) + "$" + type.asElement().getSimpleName();
			}
			String qualifiedName = getQualifiedName(type.asElement());
			if (type.getTypeArguments().isEmpty()) {
				return qualifiedName;
			}
			StringBuilder name = new StringBuilder();
			name.append(qualifiedName);
			name.append("<")
					.append(type.getTypeArguments().stream().map(TypeMirror::toString).collect(Collectors.joining(",")))
					.append(">");
			return name.toString();
		}

		@Override
		public String visitArray(ArrayType t, Void none) {
			return t.getComponentType().accept(this, none) + "[]";
		}

		@Override
		public String visitPrimitive(PrimitiveType t, Void none) {
			return this.types.boxedClass(t).getQualifiedName().toString();
		}

		public String getQualifiedName(Element element) {
			if (element == null) {
				return null;
			}
			TypeElement enclosingElement = getEnclosingTypeElement(element.asType());
			if (enclosingElement != null) {
				return getQualifiedName(enclosingElement) + "$"
						+ ((DeclaredType) element.asType()).asElement().getSimpleName();
			}
			if (element instanceof TypeElement) {
				return ((TypeElement) element).getQualifiedName().toString();
			}
			throw new IllegalStateException("Could not extract qualified name from " + element);
		}

		private TypeElement getEnclosingTypeElement(TypeMirror type) {
			if (type instanceof DeclaredType) {
				DeclaredType declaredType = (DeclaredType) type;
				Element enclosingElement = declaredType.asElement().getEnclosingElement();
				if (enclosingElement != null && enclosingElement instanceof TypeElement) {
					return (TypeElement) enclosingElement;
				}
			}
			return null;
		}

	}

}
