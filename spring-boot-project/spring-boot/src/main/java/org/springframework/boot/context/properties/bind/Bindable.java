/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Source that can be bound by a {@link Binder}.
 *
 * @param <T> The source type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see Bindable#of(Class)
 * @see Bindable#of(ResolvableType)
 */
public final class Bindable<T> {

	private static final Annotation[] NO_ANNOTATIONS = {};

	private final ResolvableType type;

	private final ResolvableType boxedType;

	private final Supplier<T> value;

	private final Annotation[] annotations;

	private Bindable(ResolvableType type, ResolvableType boxedType, Supplier<T> value,
			Annotation[] annotations) {
		this.type = type;
		this.boxedType = boxedType;
		this.value = value;
		this.annotations = annotations;
	}

	/**
	 * Return the type of the item to bind.
	 * @return the type being bound
	 */
	public ResolvableType getType() {
		return this.type;
	}

	/**
	 * Return the boxed type of the item to bind.
	 * @return the boxed type for the item being bound
	 */
	public ResolvableType getBoxedType() {
		return this.boxedType;
	}

	/**
	 * Return a supplier that provides the object value or {@code null}.
	 * @return the value or {@code null}
	 */
	public Supplier<T> getValue() {
		return this.value;
	}

	/**
	 * Return any associated annotations that could affect binding.
	 * @return the associated annotations
	 */
	public Annotation[] getAnnotations() {
		return this.annotations;
	}

	/**
	 * Return a single associated annotations that could affect binding.
	 * @param <A> the annotation type
	 * @param type annotation type
	 * @return the associated annotation or {@code null}
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		for (Annotation annotation : this.annotations) {
			if (type.isInstance(annotation)) {
				return (A) annotation;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("type", this.type);
		creator.append("value", (this.value == null ? "none" : "provided"));
		creator.append("annotations", this.annotations);
		return creator.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.type);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.annotations);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Bindable<?> other = (Bindable<?>) obj;
		boolean result = true;
		result = result && nullSafeEquals(this.type.resolve(), other.type.resolve());
		result = result && nullSafeEquals(this.annotations, other.annotations);
		return result;
	}

	private boolean nullSafeEquals(Object o1, Object o2) {
		return ObjectUtils.nullSafeEquals(o1, o2);
	}

	/**
	 * Create an updated {@link Bindable} instance with the specified annotations.
	 * @param annotations the annotations
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withAnnotations(Annotation... annotations) {
		return new Bindable<>(this.type, this.boxedType, this.value,
				(annotations == null ? NO_ANNOTATIONS : annotations));
	}

	/**
	 * Create an updated {@link Bindable} instance with an existing value.
	 * @param existingValue the existing value
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withExistingValue(T existingValue) {
		Assert.isTrue(
				existingValue == null || this.type.isArray()
						|| this.boxedType.resolve().isInstance(existingValue),
				() -> "ExistingValue must be an instance of " + this.type);
		Supplier<T> value = (existingValue == null ? null : () -> existingValue);
		return new Bindable<>(this.type, this.boxedType, value, NO_ANNOTATIONS);
	}

	/**
	 * Create an updated {@link Bindable} instance with a value supplier.
	 * @param suppliedValue the supplier for the value
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withSuppliedValue(Supplier<T> suppliedValue) {
		return new Bindable<>(this.type, this.boxedType, suppliedValue, NO_ANNOTATIONS);
	}

	/**
	 * Create a new {@link Bindable} of the type of the specified instance with an
	 * existing value equal to the instance.
	 * @param <T> The source type
	 * @param instance the instance (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType)
	 * @see #withExistingValue(Object)
	 */
	@SuppressWarnings("unchecked")
	public static <T> Bindable<T> ofInstance(T instance) {
		Assert.notNull(instance, "Instance must not be null");
		Class<T> type = (Class<T>) instance.getClass();
		return of(type).withExistingValue(instance);
	}

	/**
	 * Create a new {@link Bindable} of the specified type.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType)
	 */
	public static <T> Bindable<T> of(Class<T> type) {
		Assert.notNull(type, "Type must not be null");
		return of(ResolvableType.forClass(type));
	}

	/**
	 * Create a new {@link Bindable} {@link List} of the specified element type.
	 * @param <E> the element type
	 * @param elementType the list element type
	 * @return a {@link Bindable} instance
	 */
	public static <E> Bindable<List<E>> listOf(Class<E> elementType) {
		return of(ResolvableType.forClassWithGenerics(List.class, elementType));
	}

	/**
	 * Create a new {@link Bindable} {@link Set} of the specified element type.
	 * @param <E> the element type
	 * @param elementType the set element type
	 * @return a {@link Bindable} instance
	 */
	public static <E> Bindable<Set<E>> setOf(Class<E> elementType) {
		return of(ResolvableType.forClassWithGenerics(Set.class, elementType));
	}

	/**
	 * Create a new {@link Bindable} {@link Map} of the specified key and value type.
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param keyType the map key type
	 * @param valueType the map value type
	 * @return a {@link Bindable} instance
	 */
	public static <K, V> Bindable<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
		return of(ResolvableType.forClassWithGenerics(Map.class, keyType, valueType));
	}

	/**
	 * Create a new {@link Bindable} of the specified type.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(Class)
	 */
	public static <T> Bindable<T> of(ResolvableType type) {
		Assert.notNull(type, "Type must not be null");
		ResolvableType boxedType = box(type);
		return new Bindable<>(type, boxedType, null, NO_ANNOTATIONS);
	}

	private static ResolvableType box(ResolvableType type) {
		Class<?> resolved = type.resolve();
		if (resolved != null && resolved.isPrimitive()) {
			Object array = Array.newInstance(resolved, 1);
			Class<?> wrapperType = Array.get(array, 0).getClass();
			return ResolvableType.forClass(wrapperType);
		}
		if (resolved != null && resolved.isArray()) {
			return ResolvableType.forArrayComponent(box(type.getComponentType()));
		}
		return type;
	}

}
