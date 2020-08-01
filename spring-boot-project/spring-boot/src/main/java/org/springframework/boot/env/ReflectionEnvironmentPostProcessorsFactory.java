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

package org.springframework.boot.env;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link EnvironmentPostProcessorsFactory} implementation that uses reflection to create
 * instances.
 *
 * @author Phillip Webb
 */
class ReflectionEnvironmentPostProcessorsFactory implements EnvironmentPostProcessorsFactory {

	private final List<String> classNames;

	ReflectionEnvironmentPostProcessorsFactory(Class<?>... classes) {
		this(Arrays.stream(classes).map(Class::getName).toArray(String[]::new));
	}

	ReflectionEnvironmentPostProcessorsFactory(String... classNames) {
		this(Arrays.asList(classNames));
	}

	ReflectionEnvironmentPostProcessorsFactory(List<String> classNames) {
		this.classNames = classNames;
	}

	@Override
	public List<EnvironmentPostProcessor> getEnvironmentPostProcessors(DeferredLogFactory logFactory) {
		List<EnvironmentPostProcessor> postProcessors = new ArrayList<>(this.classNames.size());
		for (String className : this.classNames) {
			try {
				postProcessors.add(getEnvironmentPostProcessor(className, logFactory));
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Unable to instantiate factory class [" + className
						+ "] for factory type [" + EnvironmentPostProcessor.class.getName() + "]", ex);
			}
		}
		AnnotationAwareOrderComparator.sort(postProcessors);
		return postProcessors;
	}

	private EnvironmentPostProcessor getEnvironmentPostProcessor(String className, DeferredLogFactory logFactory)
			throws Exception {
		Class<?> type = ClassUtils.forName(className, getClass().getClassLoader());
		Assert.isAssignable(EnvironmentPostProcessor.class, type);
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		for (Constructor<?> constructor : constructors) {
			if (constructor.getParameterCount() == 1) {
				Class<?> cls = constructor.getParameterTypes()[0];
				if (DeferredLogFactory.class.isAssignableFrom(cls)) {
					return newInstance(constructor, logFactory);
				}
				if (Log.class.isAssignableFrom(cls)) {
					return newInstance(constructor, logFactory.getLog(type));
				}
			}
		}
		return (EnvironmentPostProcessor) ReflectionUtils.accessibleConstructor(type).newInstance();
	}

	private EnvironmentPostProcessor newInstance(Constructor<?> constructor, Object... initargs) throws Exception {
		ReflectionUtils.makeAccessible(constructor);
		return (EnvironmentPostProcessor) constructor.newInstance(initargs);
	}

}
