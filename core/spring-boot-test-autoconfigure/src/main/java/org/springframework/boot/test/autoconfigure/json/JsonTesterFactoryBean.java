/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import java.lang.reflect.Constructor;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ReflectionUtils;

/**
 * {@link FactoryBean} used to create JSON Tester instances.
 *
 * @param <T> the object type
 * @param <M> the marshaller type
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class JsonTesterFactoryBean<T, M> implements FactoryBean<T> {

	private final Class<?> objectType;

	private final @Nullable M marshaller;

	public JsonTesterFactoryBean(Class<?> objectType, @Nullable M marshaller) {
		this.objectType = objectType;
		this.marshaller = marshaller;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getObject() throws Exception {
		if (this.marshaller == null) {
			Constructor<?> constructor = this.objectType.getDeclaredConstructor();
			ReflectionUtils.makeAccessible(constructor);
			return (T) BeanUtils.instantiateClass(constructor);
		}
		Constructor<?>[] constructors = this.objectType.getDeclaredConstructors();
		for (Constructor<?> constructor : constructors) {
			if (constructor.getParameterCount() == 1
					&& constructor.getParameterTypes()[0].isInstance(this.marshaller)) {
				ReflectionUtils.makeAccessible(constructor);
				return (T) BeanUtils.instantiateClass(constructor, this.marshaller);
			}
		}
		throw new IllegalStateException(this.objectType + " does not have a usable constructor");
	}

	@Override
	public Class<?> getObjectType() {
		return this.objectType;
	}

}
