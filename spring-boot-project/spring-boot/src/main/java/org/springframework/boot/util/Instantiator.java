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

package org.springframework.boot.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Simple factory used to instantiate objects by injecting available parameters.
 *
 * @param <T> the type to instantiate
 * @author Phillip Webb
 * @since 2.4.0
 */
public class Instantiator<T> {

	private static final Comparator<Constructor<?>> CONSTRUCTOR_COMPARATOR = Comparator
			.<Constructor<?>>comparingInt(Constructor::getParameterCount).reversed();

	private final Class<?> type;

	private final Map<Class<?>, Function<Class<?>, Object>> availableParameters;

	/**
	 * Create a new {@link Instantiator} instance for the given type.
	 * @param type the type to instantiate
	 * @param availableParameters consumer used to register available parameters
	 */
	public Instantiator(Class<?> type, Consumer<AvailableParameters> availableParameters) {
		this.type = type;
		this.availableParameters = getAvailableParameters(availableParameters);
	}

	private Map<Class<?>, Function<Class<?>, Object>> getAvailableParameters(
			Consumer<AvailableParameters> availableParameters) {
		Map<Class<?>, Function<Class<?>, Object>> result = new LinkedHashMap<>();
		availableParameters.accept(new AvailableParameters() {

			@Override
			public void add(Class<?> type, Object instance) {
				result.put(type, (factoryType) -> instance);
			}

			@Override
			public void add(Class<?> type, Function<Class<?>, Object> factory) {
				result.put(type, factory);
			}

		});
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Instantiate the given set of class name, injecting constructor arguments as
	 * necessary.
	 * @param names the class names to instantiate
	 * @return a list of instantiated instances
	 */
	public List<T> instantiate(Collection<String> names) {
		List<T> instances = new ArrayList<>(names.size());
		for (String name : names) {
			instances.add(instantiate(name));
		}
		AnnotationAwareOrderComparator.sort(instances);
		return Collections.unmodifiableList(instances);
	}

	private T instantiate(String name) {
		try {
			Class<?> type = ClassUtils.forName(name, null);
			Assert.isAssignable(this.type, type);
			return instantiate(type);
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Unable to instantiate " + this.type.getName() + " [" + name + "]", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private T instantiate(Class<?> type) throws Exception {
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		Arrays.sort(constructors, CONSTRUCTOR_COMPARATOR);
		for (Constructor<?> constructor : constructors) {
			Object[] args = getArgs(constructor.getParameterTypes());
			if (args != null) {
				ReflectionUtils.makeAccessible(constructor);
				return (T) constructor.newInstance(args);
			}
		}
		throw new IllegalAccessException("Unable to find suitable constructor");
	}

	private Object[] getArgs(Class<?>[] parameterTypes) {
		Object[] args = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			Function<Class<?>, Object> parameter = getAvailableParameter(parameterTypes[i]);
			if (parameter == null) {
				return null;
			}
			args[i] = parameter.apply(this.type);
		}
		return args;
	}

	private Function<Class<?>, Object> getAvailableParameter(Class<?> parameterType) {
		for (Map.Entry<Class<?>, Function<Class<?>, Object>> entry : this.availableParameters.entrySet()) {
			if (entry.getKey().isAssignableFrom(parameterType)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Callback used to register available parameters.
	 */
	public interface AvailableParameters {

		/**
		 * Add a parameter with an instance value.
		 * @param type the parameter type
		 * @param instance the instance that should be injected
		 */
		void add(Class<?> type, Object instance);

		/**
		 * Add a parameter with an instance factory.
		 * @param type the parameter type
		 * @param factory the factory used to create the instance that should be injected
		 */
		void add(Class<?> type, Function<Class<?>, Object> factory);

	}

}
