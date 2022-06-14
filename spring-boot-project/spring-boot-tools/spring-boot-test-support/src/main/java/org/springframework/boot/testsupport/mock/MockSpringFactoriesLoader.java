/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.testsupport.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Simple mock {@link SpringFactoriesLoader} implementation that can be used for testing
 * purposes.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
public class MockSpringFactoriesLoader extends SpringFactoriesLoader {

	private final AtomicInteger sequence = new AtomicInteger();

	private final Map<String, List<String>> factories;

	private final Map<String, Object> implementations = new HashMap<>();

	/**
	 * Create a new {@link MockSpringFactoriesLoader} instance with the default
	 * classloader.
	 */
	public MockSpringFactoriesLoader() {
		this(null);
	}

	/**
	 * Create a new {@link MockSpringFactoriesLoader} instance with the given classloader.
	 * @param classLoader the classloader to use
	 */
	public MockSpringFactoriesLoader(ClassLoader classLoader) {
		this(classLoader, new LinkedHashMap<>());
	}

	protected MockSpringFactoriesLoader(ClassLoader classLoader, Map<String, List<String>> factories) {
		super(classLoader, factories);
		this.factories = factories;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T> T instantiateFactory(String implementationName, Class<T> type, ArgumentResolver argumentResolver,
			FailureHandler failureHandler) {
		if (implementationName.startsWith("!")) {
			Object implementation = this.implementations.get(implementationName);
			if (implementation != null) {
				return (T) implementation;
			}
		}
		return super.instantiateFactory(implementationName, type, argumentResolver, failureHandler);
	}

	/**
	 * Add factory implementations to this instance.
	 * @param factoryType the factory type class
	 * @param factoryImplementations the implementation classes
	 * @param <T> the factory type
	 * @return {@code this}, to facilitate method chaining
	 */
	@SafeVarargs
	public final <T> MockSpringFactoriesLoader add(Class<T> factoryType, Class<? extends T>... factoryImplementations) {
		for (Class<? extends T> factoryImplementation : factoryImplementations) {
			add(factoryType.getName(), factoryImplementation.getName());
		}
		return this;
	}

	/**
	 * Add factory implementations to this instance.
	 * @param factoryType the factory type class name
	 * @param factoryImplementations the implementation class names
	 * @return {@code this}, to facilitate method chaining
	 */
	public MockSpringFactoriesLoader add(String factoryType, String... factoryImplementations) {
		List<String> implementations = this.factories.computeIfAbsent(factoryType, (key) -> new ArrayList<>());
		for (String factoryImplementation : factoryImplementations) {
			implementations.add(factoryImplementation);
		}
		return this;
	}

	/**
	 * Add factory instances to this instance.
	 * @param factoryType the factory type class
	 * @param factoryInstances the implementation instances to add
	 * @param <T> the factory type
	 * @return {@code this}, to facilitate method chaining
	 */
	@SuppressWarnings("unchecked")
	public <T> MockSpringFactoriesLoader addInstance(Class<T> factoryType, T... factoryInstances) {
		return addInstance(factoryType.getName(), factoryInstances);
	}

	/**
	 * Add factory instances to this instance.
	 * @param factoryType the factory type class name
	 * @param factoryInstance the implementation instances to add
	 * @param <T> the factory instance type
	 * @return {@code this}, to facilitate method chaining
	 */
	@SuppressWarnings("unchecked")
	public <T> MockSpringFactoriesLoader addInstance(String factoryType, T... factoryInstance) {
		List<String> implementations = this.factories.computeIfAbsent(factoryType, (key) -> new ArrayList<>());
		for (T factoryImplementation : factoryInstance) {
			String reference = "!" + factoryType + ":" + factoryImplementation.getClass().getName()
					+ this.sequence.getAndIncrement();
			implementations.add(reference);
			this.implementations.put(reference, factoryImplementation);
		}
		return this;
	}

}
