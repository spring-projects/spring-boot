/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
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

	private static final Pattern NEW_LINE_PATTERN = Pattern.compile("[\r\n]+");

	static {
		Map<String, TypeKind> primitives = new HashMap<>();
		PRIMITIVE_WRAPPERS.forEach((kind, wrapperClass) -> primitives.put(wrapperClass.getName(), kind));
		WRAPPER_TO_PRIMITIVE = primitives;
	}

	private final ProcessingEnvironment env;

	private final Types types;

	private final TypeExtractor typeExtractor;

	private final TypeMirror collectionType;

	private final TypeMirror mapType;

	private final Map<TypeElement, TypeDescriptor> typeDescriptors = new HashMap<>();

	TypeUtils(ProcessingEnvironment env) {
		this.env = env;
		this.types = env.getTypeUtils();
		this.typeExtractor = new TypeExtractor(this.types);
		this.collectionType = getDeclaredType(this.types, Collection.class, 1);
		this.mapType = getDeclaredType(this.types, Map.class, 2);
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

	boolean isSameType(TypeMirror t1, TypeMirror t2) {
		return this.types.isSameType(t1, t2);
	}

	Element asElement(TypeMirror type) {
		return this.types.asElement(type);
	}

	/**
	 * Return the qualified name of the specified element.
	 * @param element the element to handle
	 * @return the fully qualified name of the element, suitable for a call to
	 * {@link Class#forName(String)}
	 */
	String getQualifiedName(Element element) {
		return this.typeExtractor.getQualifiedName(element);
	}

	/**
	 * Return the type of the specified {@link TypeMirror} including all its generic
	 * information.
	 * @param element the {@link TypeElement} in which this {@code type} is declared
	 * @param type the type to handle
	 * @return a representation of the type including all its generic information
	 */
	String getType(TypeElement element, TypeMirror type) {
		if (type == null) {
			return null;
		}
		return type.accept(this.typeExtractor, createTypeDescriptor(element));
	}

	/**
	 * Extract the target element type from the specified container type or {@code null}
	 * if no element type was found.
	 * @param type a type, potentially wrapping an element type
	 * @return the element type or {@code null} if no specific type was found
	 */
	TypeMirror extractElementType(TypeMirror type) {
		if (!this.env.getTypeUtils().isAssignable(type, this.collectionType)) {
			return null;
		}
		return getCollectionElementType(type);
	}

	private TypeMirror getCollectionElementType(TypeMirror type) {
		if (((TypeElement) this.types.asElement(type)).getQualifiedName().contentEquals(Collection.class.getName())) {
			DeclaredType declaredType = (DeclaredType) type;
			// raw type, just "Collection"
			if (declaredType.getTypeArguments().isEmpty()) {
				return this.types.getDeclaredType(this.env.getElementUtils().getTypeElement(Object.class.getName()));
			}
			// return type argument to Collection<...>
			return declaredType.getTypeArguments().get(0);
		}

		// recursively walk the supertypes, looking for Collection<...>
		for (TypeMirror superType : this.env.getTypeUtils().directSupertypes(type)) {
			if (this.types.isAssignable(superType, this.collectionType)) {
				return getCollectionElementType(superType);
			}
		}
		return null;
	}

	boolean isCollectionOrMap(TypeMirror type) {
		return this.env.getTypeUtils().isAssignable(type, this.collectionType)
				|| this.env.getTypeUtils().isAssignable(type, this.mapType);
	}

	String getJavaDoc(Element element) {
		String javadoc = (element != null) ? this.env.getElementUtils().getDocComment(element) : null;
		if (javadoc != null) {
			javadoc = NEW_LINE_PATTERN.matcher(javadoc).replaceAll("").trim();
		}
		return "".equals(javadoc) ? null : javadoc;
	}

	/**
	 * Return the {@link PrimitiveType} of the specified type or {@code null} if the type
	 * does not represent a valid wrapper type.
	 * @param typeMirror a type
	 * @return the primitive type or {@code null} if the type is not a wrapper type
	 */
	PrimitiveType getPrimitiveType(TypeMirror typeMirror) {
		if (getPrimitiveFor(typeMirror) != null) {
			return this.types.unboxedType(typeMirror);
		}
		return null;
	}

	TypeMirror getWrapperOrPrimitiveFor(TypeMirror typeMirror) {
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

	TypeDescriptor resolveTypeDescriptor(TypeElement element) {
		if (this.typeDescriptors.containsKey(element)) {
			return this.typeDescriptors.get(element);
		}
		return createTypeDescriptor(element);
	}

	private TypeDescriptor createTypeDescriptor(TypeElement element) {
		TypeDescriptor descriptor = new TypeDescriptor();
		process(descriptor, element.asType());
		this.typeDescriptors.put(element, descriptor);
		return descriptor;
	}

	private void process(TypeDescriptor descriptor, TypeMirror type) {
		if (type.getKind() == TypeKind.DECLARED) {
			DeclaredType declaredType = (DeclaredType) type;
			DeclaredType freshType = (DeclaredType) this.env.getElementUtils()
					.getTypeElement(this.types.asElement(type).toString()).asType();
			List<? extends TypeMirror> arguments = declaredType.getTypeArguments();
			for (int i = 0; i < arguments.size(); i++) {
				TypeMirror specificType = arguments.get(i);
				TypeMirror signatureType = freshType.getTypeArguments().get(i);
				descriptor.registerIfNecessary(signatureType, specificType);
			}
			TypeElement element = (TypeElement) this.types.asElement(type);
			process(descriptor, element.getSuperclass());
		}
	}

	/**
	 * A visitor that extracts the fully qualified name of a type, including generic
	 * information.
	 */
	private static class TypeExtractor extends SimpleTypeVisitor8<String, TypeDescriptor> {

		private final Types types;

		TypeExtractor(Types types) {
			this.types = types;
		}

		@Override
		public String visitDeclared(DeclaredType type, TypeDescriptor descriptor) {
			TypeElement enclosingElement = getEnclosingTypeElement(type);
			String qualifiedName = determineQualifiedName(type, enclosingElement);
			if (type.getTypeArguments().isEmpty()) {
				return qualifiedName;
			}
			StringBuilder name = new StringBuilder();
			name.append(qualifiedName);
			name.append("<").append(
					type.getTypeArguments().stream().map((t) -> visit(t, descriptor)).collect(Collectors.joining(",")))
					.append(">");
			return name.toString();
		}

		private String determineQualifiedName(DeclaredType type, TypeElement enclosingElement) {
			if (enclosingElement != null) {
				return getQualifiedName(enclosingElement) + "$" + type.asElement().getSimpleName();
			}
			return getQualifiedName(type.asElement());
		}

		@Override
		public String visitTypeVariable(TypeVariable t, TypeDescriptor descriptor) {
			TypeMirror typeMirror = descriptor.resolveGeneric(t);
			if (typeMirror != null) {
				if (typeMirror instanceof TypeVariable) {
					TypeVariable typeVariable = (TypeVariable) typeMirror;
					// Still unresolved, let's use the upper bound, checking first if
					// a cycle may exist
					if (!hasCycle(typeVariable)) {
						return visit(typeVariable.getUpperBound(), descriptor);
					}
				}
				else {
					return visit(typeMirror, descriptor);
				}
			}
			// Fallback to simple representation of the upper bound
			return defaultAction(t.getUpperBound(), descriptor);
		}

		private boolean hasCycle(TypeVariable variable) {
			TypeMirror upperBound = variable.getUpperBound();
			if (upperBound instanceof DeclaredType) {
				return ((DeclaredType) upperBound).getTypeArguments().stream()
						.anyMatch((candidate) -> candidate.equals(variable));
			}
			return false;
		}

		@Override
		public String visitArray(ArrayType t, TypeDescriptor descriptor) {
			return t.getComponentType().accept(this, descriptor) + "[]";
		}

		@Override
		public String visitPrimitive(PrimitiveType t, TypeDescriptor descriptor) {
			return this.types.boxedClass(t).getQualifiedName().toString();
		}

		@Override
		protected String defaultAction(TypeMirror t, TypeDescriptor descriptor) {
			return t.toString();
		}

		String getQualifiedName(Element element) {
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
				if (enclosingElement instanceof TypeElement) {
					return (TypeElement) enclosingElement;
				}
			}
			return null;
		}

	}

	/**
	 * Descriptor for a given type.
	 */
	static class TypeDescriptor {

		private final Map<TypeVariable, TypeMirror> generics = new HashMap<>();

		Map<TypeVariable, TypeMirror> getGenerics() {
			return Collections.unmodifiableMap(this.generics);
		}

		TypeMirror resolveGeneric(TypeVariable typeVariable) {
			return resolveGeneric(getParameterName(typeVariable));
		}

		TypeMirror resolveGeneric(String parameterName) {
			return this.generics.entrySet().stream().filter((e) -> getParameterName(e.getKey()).equals(parameterName))
					.findFirst().map(Entry::getValue).orElse(null);
		}

		private void registerIfNecessary(TypeMirror variable, TypeMirror resolution) {
			if (variable instanceof TypeVariable) {
				TypeVariable typeVariable = (TypeVariable) variable;
				if (this.generics.keySet().stream()
						.noneMatch((candidate) -> getParameterName(candidate).equals(getParameterName(typeVariable)))) {
					this.generics.put(typeVariable, resolution);
				}
			}
		}

		private String getParameterName(TypeVariable typeVariable) {
			return typeVariable.asElement().getSimpleName().toString();
		}

	}

}
