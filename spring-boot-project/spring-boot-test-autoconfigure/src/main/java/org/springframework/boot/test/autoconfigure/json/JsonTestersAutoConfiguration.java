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

package org.springframework.boot.test.autoconfigure.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.json.bind.Jsonb;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.test.json.AbstractJsonMarshalTester;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonbTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ResolvableType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Auto-configuration for Json testers.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @since 1.4.0
 * @see AutoConfigureJsonTesters
 */
@AutoConfiguration(
		after = { JacksonAutoConfiguration.class, GsonAutoConfiguration.class, JsonbAutoConfiguration.class })
@ConditionalOnClass(name = "org.assertj.core.api.Assert")
@ConditionalOnProperty("spring.test.jsontesters.enabled")
public class JsonTestersAutoConfiguration {

	@Bean
	public static JsonMarshalTestersBeanPostProcessor jsonMarshalTestersBeanPostProcessor() {
		return new JsonMarshalTestersBeanPostProcessor();
	}

	@Bean
	@Scope("prototype")
	@ImportRuntimeHints(BasicJsonTesterRuntimeHints.class)
	public FactoryBean<BasicJsonTester> basicJsonTesterFactoryBean() {
		return new JsonTesterFactoryBean<BasicJsonTester, Void>(BasicJsonTester.class, null);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	static class JacksonJsonTestersConfiguration {

		@Bean
		@Scope("prototype")
		@ConditionalOnBean(ObjectMapper.class)
		@ImportRuntimeHints(JacksonTesterRuntimeHints.class)
		FactoryBean<JacksonTester<?>> jacksonTesterFactoryBean(ObjectMapper mapper) {
			return new JsonTesterFactoryBean<>(JacksonTester.class, mapper);
		}

		static class JacksonTesterRuntimeHints extends AbstractJsonMarshalTesterRuntimeHints {

			JacksonTesterRuntimeHints() {
				super(JacksonTester.class);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Gson.class)
	static class GsonJsonTestersConfiguration {

		@Bean
		@Scope("prototype")
		@ConditionalOnBean(Gson.class)
		@ImportRuntimeHints(GsonTesterRuntimeHints.class)
		FactoryBean<GsonTester<?>> gsonTesterFactoryBean(Gson gson) {
			return new JsonTesterFactoryBean<>(GsonTester.class, gson);
		}

		static class GsonTesterRuntimeHints extends AbstractJsonMarshalTesterRuntimeHints {

			GsonTesterRuntimeHints() {
				super(GsonTester.class);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Jsonb.class)
	static class JsonbJsonTesterConfiguration {

		@Bean
		@Scope("prototype")
		@ConditionalOnBean(Jsonb.class)
		@ImportRuntimeHints(JsonbJsonTesterRuntimeHints.class)
		FactoryBean<JsonbTester<?>> jsonbTesterFactoryBean(Jsonb jsonb) {
			return new JsonTesterFactoryBean<>(JsonbTester.class, jsonb);
		}

		static class JsonbJsonTesterRuntimeHints extends AbstractJsonMarshalTesterRuntimeHints {

			JsonbJsonTesterRuntimeHints() {
				super(JsonbTester.class);
			}

		}

	}

	/**
	 * {@link FactoryBean} used to create JSON Tester instances.
	 *
	 * @param <T> the object type
	 * @param <M> the marshaller type
	 */
	static class JsonTesterFactoryBean<T, M> implements FactoryBean<T> {

		private final Class<?> objectType;

		private final M marshaller;

		JsonTesterFactoryBean(Class<?> objectType, M marshaller) {
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

	/**
	 * {@link BeanPostProcessor} used to initialize JSON testers.
	 */
	static class JsonMarshalTestersBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			ReflectionUtils.doWithFields(bean.getClass(), (field) -> processField(bean, field));
			return bean;
		}

		private void processField(Object bean, Field field) {
			if (AbstractJsonMarshalTester.class.isAssignableFrom(field.getType())) {
				initializeTester(bean, field, bean.getClass(), ResolvableType.forField(field).getGeneric());
			}
			else if (BasicJsonTester.class.isAssignableFrom(field.getType())) {
				initializeTester(bean, field, bean.getClass());
			}
		}

		private void initializeTester(Object bean, Field field, Object... args) {
			ReflectionUtils.makeAccessible(field);
			Object tester = ReflectionUtils.getField(field, bean);
			if (tester != null) {
				ReflectionTestUtils.invokeMethod(tester, "initialize", args);
			}
		}

	}

	@SuppressWarnings("rawtypes")
	static class AbstractJsonMarshalTesterRuntimeHints implements RuntimeHintsRegistrar {

		private final Class<? extends AbstractJsonMarshalTester> tester;

		AbstractJsonMarshalTesterRuntimeHints(Class<? extends AbstractJsonMarshalTester> tester) {
			this.tester = tester;
		}

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			ReflectionHints reflection = hints.reflection();
			reflection.registerType(this.tester, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			reflection.registerMethod(
					ReflectionUtils.findMethod(this.tester, "initialize", Class.class, ResolvableType.class),
					ExecutableMode.INVOKE);
		}

	}

	static class BasicJsonTesterRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			ReflectionHints reflection = hints.reflection();
			reflection.registerType(BasicJsonTester.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			reflection.registerMethod(ReflectionUtils.findMethod(BasicJsonTester.class, "initialize", Class.class),
					ExecutableMode.INVOKE);
		}

	}

}
