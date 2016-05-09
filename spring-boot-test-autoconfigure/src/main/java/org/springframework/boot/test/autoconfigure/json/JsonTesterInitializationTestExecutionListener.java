/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * {@link TestExecutionListener} to initialize JSON tester fields.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class JsonTesterInitializationTestExecutionListener
		extends AbstractTestExecutionListener {

	private static final String ASSERTJ_CLASS = "org.assertj.core.api.Assert";

	private static final Map<String, Class<?>> INITIALIZERS;

	static {
		Map<String, Class<?>> initializers = new LinkedHashMap<String, Class<?>>();
		initializers.put("com.fasterxml.jackson.databind.ObjectMapper",
				JacksonInitializer.class);
		initializers.put("com.google.gson.Gson", GsonInitializer.class);
		INITIALIZERS = Collections.unmodifiableMap(initializers);
	}

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		if (ClassUtils.isPresent(ASSERTJ_CLASS, classLoader)
				&& shouldInitializeFields(testContext)) {
			initializeBasicJsonTesterFields(testContext);
			initializeJsonMarshalTesterFields(classLoader, testContext);
		}
	}

	private boolean shouldInitializeFields(TestContext testContext) {
		AutoConfigureJsonTesters annotation = AnnotatedElementUtils.getMergedAnnotation(
				testContext.getTestClass(), AutoConfigureJsonTesters.class);
		return (annotation != null && annotation.initFields());
	}

	private void initializeBasicJsonTesterFields(final TestContext testContext) {
		ReflectionUtils.doWithFields(testContext.getTestClass(), new FieldCallback() {

			@Override
			public void doWith(Field field)
					throws IllegalArgumentException, IllegalAccessException {
				if (BasicJsonTester.class.isAssignableFrom(field.getType())) {
					setupField(field);
				}
			}

			private void setupField(Field field) {
				ReflectionUtils.makeAccessible(field);
				Object existingInstance = ReflectionUtils.getField(field,
						testContext.getTestInstance());
				if (existingInstance == null) {
					ReflectionUtils.setField(field, testContext.getTestInstance(),
							new BasicJsonTester(testContext.getTestClass()));
				}
			}

		});
	}

	private void initializeJsonMarshalTesterFields(ClassLoader classLoader,
			TestContext testContext) {
		for (Map.Entry<String, Class<?>> entry : INITIALIZERS.entrySet()) {
			if (ClassUtils.isPresent(entry.getKey(), classLoader)) {
				initializeJsonMarshalTesterFields(classLoader, testContext,
						entry.getKey(), entry.getValue());
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initializeJsonMarshalTesterFields(ClassLoader classLoader,
			TestContext testContext, String marshallerClassName, Class<?> initializer) {
		try {
			Constructor<?> constructor = initializer.getDeclaredConstructor();
			ReflectionUtils.makeAccessible(constructor);
			initializeJsonMarshalTesterFields(testContext,
					ClassUtils.resolveClassName(marshallerClassName, classLoader),
					(Initializer) constructor.newInstance());
		}
		catch (Throwable ex) {
			throw new IllegalStateException(ex);
		}
	}

	private <T> void initializeJsonMarshalTesterFields(final TestContext testContext,
			final Class<T> marshallerClass, Initializer<T> initializer) {
		initializer.initialize(testContext, new ObjectFactory<T>() {

			@Override
			public T getObject() throws BeansException {
				return testContext.getApplicationContext().getBean(marshallerClass);
			}

		});
	}

	/**
	 * Strategy used to initialize JSON testers without cause class not found exceptions.
	 * @param <M> the marshaller type
	 */
	interface Initializer<M> {

		void initialize(TestContext testContext, ObjectFactory<M> marshaller);

	}

	/**
	 * {@link Initializer} for {@link JacksonTester}.
	 */
	static class JacksonInitializer implements Initializer<ObjectMapper> {

		@Override
		public void initialize(TestContext testContext,
				ObjectFactory<ObjectMapper> marshaller) {
			JacksonTester.initFields(testContext.getTestInstance(), marshaller);
		}

	}

	/**
	 * {@link Initializer} for {@link GsonTester}.
	 */
	static class GsonInitializer implements Initializer<Gson> {

		@Override
		public void initialize(TestContext testContext, ObjectFactory<Gson> marshaller) {
			GsonTester.initFields(testContext.getTestInstance(), marshaller);
		}

	}

}
