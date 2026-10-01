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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.RecordComponentElement;
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
 * @author Pavel Anisimov
 * @author Dmytro Nosan
 */
class TypeUtils {

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
		return type.accept(this.typeExtractor, resolveTypeDescriptor(element));
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
		if (element instanceof RecordComponentElement) {
			return getJavaDoc((RecordComponentElement) element);
		}
		return (element != null) ? this.env.getElementUtils().getDocComment(element) : null;
	}

	private TypeDescriptor resolveTypeDescriptor(TypeElement element) {
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
				.getTypeElement(this.types.asElement(type).toString())
				.asType();
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

	private String getJavaDoc(RecordComponentElement recordComponent) {
		String recordJavadoc = this.env.getElementUtils().getDocComment(recordComponent.getEnclosingElement());
		if (recordJavadoc != null) {
			Pattern paramJavadocPattern = paramJavadocPattern(recordComponent.getSimpleName().toString());
			Matcher paramJavadocMatcher = paramJavadocPattern.matcher(recordJavadoc);
			if (paramJavadocMatcher.find()) {
				return paramJavadocMatcher.group();
			}
		}
		return null;
	}

	private Pattern paramJavadocPattern(String paramName) {
		String pattern = String.format("(?<=@param +%s).*?(?=([\r\n]+ *@)|$)", paramName);
		return Pattern.compile(pattern, Pattern.DOTALL);
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
			name.append("<")
				.append(type.getTypeArguments()
					.stream()
					.map((t) -> visit(t, descriptor))
					.collect(Collectors.joining(",")))
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
		public String visitTypeVariable(TypeVariable typeVariable, TypeDescriptor descriptor) {
			TypeMirror resolvedGeneric = descriptor.resolveGeneric(typeVariable);
			if (resolvedGeneric != null) {
				if (resolvedGeneric instanceof TypeVariable resolveTypeVariable) {
					// Still unresolved, let's use the upper bound, checking first if
					// a cycle may exist
					if (!hasCycle(resolveTypeVariable)) {
						return visit(resolveTypeVariable.getUpperBound(), descriptor);
					}
				}
				else {
					return visit(resolvedGeneric, descriptor);
				}
			}
			// Fallback to simple representation of the upper bound
			return defaultAction(typeVariable.getUpperBound(), descriptor);
		}

		private boolean hasCycle(TypeVariable variable) {
			TypeMirror upperBound = variable.getUpperBound();
			if (upperBound instanceof DeclaredType declaredType) {
				return declaredType.getTypeArguments().stream().anyMatch((candidate) -> candidate.equals(variable));
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
			if (element instanceof TypeElement typeElement) {
				return typeElement.getQualifiedName().toString();
			}
			throw new IllegalStateException("Could not extract qualified name from " + element);
		}

		private TypeElement getEnclosingTypeElement(TypeMirror type) {
			if (type instanceof DeclaredType declaredType) {
				Element enclosingElement = declaredType.asElement().getEnclosingElement();
				if (enclosingElement instanceof TypeElement typeElement) {
					return typeElement;
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

		TypeMirror resolveGeneric(TypeVariable typeVariable) {
			TypeMirror resolved = this.generics.get(typeVariable);
			if (resolved != typeVariable && resolved instanceof TypeVariable resolvedTypeVariable) {
				return resolveGeneric(resolvedTypeVariable);
			}
			return resolved;
		}

		private void registerIfNecessary(TypeMirror variable, TypeMirror resolution) {
			if (variable instanceof TypeVariable typeVariable) {
				this.generics.put(typeVariable, resolution);
			}
		}

	}

}
