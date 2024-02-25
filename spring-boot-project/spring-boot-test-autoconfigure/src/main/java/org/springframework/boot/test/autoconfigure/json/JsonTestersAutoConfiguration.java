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

	/**
     * Creates a new instance of {@link JsonMarshalTestersBeanPostProcessor}.
     * 
     * @return the newly created {@link JsonMarshalTestersBeanPostProcessor} instance
     */
    @Bean
	public static JsonMarshalTestersBeanPostProcessor jsonMarshalTestersBeanPostProcessor() {
		return new JsonMarshalTestersBeanPostProcessor();
	}

	/**
     * Creates a factory bean for BasicJsonTester.
     * 
     * @return the factory bean for BasicJsonTester
     */
    @Bean
	@Scope("prototype")
	@ImportRuntimeHints(BasicJsonTesterRuntimeHints.class)
	public FactoryBean<BasicJsonTester> basicJsonTesterFactoryBean() {
		return new JsonTesterFactoryBean<BasicJsonTester, Void>(BasicJsonTester.class, null);
	}

	/**
     * JacksonJsonTestersConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	static class JacksonJsonTestersConfiguration {

		/**
         * Factory method for creating a {@link JacksonTester} instance as a {@link FactoryBean}.
         * This method is annotated with {@link Bean} to indicate that it should be processed by the Spring container.
         * The {@link Scope} annotation is used to specify that the created bean should have a prototype scope.
         * The {@link ConditionalOnBean} annotation ensures that this bean is only created if an {@link ObjectMapper} bean is present in the container.
         * The {@link ImportRuntimeHints} annotation is used to import the {@link JacksonTesterRuntimeHints} class, which provides runtime hints for the JacksonTester.
         * 
         * @param mapper the {@link ObjectMapper} bean to be used for creating the JacksonTester
         * @return a {@link FactoryBean} instance for creating a {@link JacksonTester} object
         */
        @Bean
		@Scope("prototype")
		@ConditionalOnBean(ObjectMapper.class)
		@ImportRuntimeHints(JacksonTesterRuntimeHints.class)
		FactoryBean<JacksonTester<?>> jacksonTesterFactoryBean(ObjectMapper mapper) {
			return new JsonTesterFactoryBean<>(JacksonTester.class, mapper);
		}

		/**
         * JacksonTesterRuntimeHints class.
         */
        static class JacksonTesterRuntimeHints extends AbstractJsonMarshalTesterRuntimeHints {

			/**
             * Constructs a new instance of JacksonTesterRuntimeHints.
             * 
             * This constructor calls the constructor of the superclass, JacksonTester, passing the class JacksonTester.class as an argument.
             */
            JacksonTesterRuntimeHints() {
				super(JacksonTester.class);
			}

		}

	}

	/**
     * GsonJsonTestersConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Gson.class)
	static class GsonJsonTestersConfiguration {

		/**
         * Creates a factory bean for GsonTester instances.
         * 
         * This method is annotated with @Bean to indicate that it is a bean definition method.
         * 
         * This method is annotated with @Scope("prototype") to indicate that a new instance of GsonTester should be created each time it is requested.
         * 
         * This method is annotated with @ConditionalOnBean(Gson.class) to indicate that the bean creation should be conditional on the presence of a bean of type Gson.
         * 
         * This method is annotated with @ImportRuntimeHints(GsonTesterRuntimeHints.class) to indicate that additional runtime hints should be imported from the GsonTesterRuntimeHints class.
         * 
         * @param gson The Gson instance to be used by the GsonTester.
         * @return A FactoryBean for GsonTester instances.
         */
        @Bean
		@Scope("prototype")
		@ConditionalOnBean(Gson.class)
		@ImportRuntimeHints(GsonTesterRuntimeHints.class)
		FactoryBean<GsonTester<?>> gsonTesterFactoryBean(Gson gson) {
			return new JsonTesterFactoryBean<>(GsonTester.class, gson);
		}

		/**
         * GsonTesterRuntimeHints class.
         */
        static class GsonTesterRuntimeHints extends AbstractJsonMarshalTesterRuntimeHints {

			/**
             * Constructs a new GsonTesterRuntimeHints object.
             * 
             * @param gsonTesterClass the class representing the GsonTester class
             */
            GsonTesterRuntimeHints() {
				super(GsonTester.class);
			}

		}

	}

	/**
     * JsonbJsonTesterConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Jsonb.class)
	static class JsonbJsonTesterConfiguration {

		/**
         * Creates a factory bean for creating instances of JsonbTester.
         * 
         * @param jsonb the Jsonb instance to be used by the JsonbTester
         * @return a FactoryBean for creating instances of JsonbTester
         */
        @Bean
		@Scope("prototype")
		@ConditionalOnBean(Jsonb.class)
		@ImportRuntimeHints(JsonbJsonTesterRuntimeHints.class)
		FactoryBean<JsonbTester<?>> jsonbTesterFactoryBean(Jsonb jsonb) {
			return new JsonTesterFactoryBean<>(JsonbTester.class, jsonb);
		}

		/**
         * JsonbJsonTesterRuntimeHints class.
         */
        static class JsonbJsonTesterRuntimeHints extends AbstractJsonMarshalTesterRuntimeHints {

			/**
             * Constructor for JsonbJsonTesterRuntimeHints class.
             * Initializes the object by calling the super constructor with the JsonbTester class.
             */
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

		/**
         * Constructs a new JsonTesterFactoryBean with the specified objectType and marshaller.
         * 
         * @param objectType the class representing the type of object to be tested
         * @param marshaller the marshaller used for marshalling and unmarshalling JSON data
         */
        JsonTesterFactoryBean(Class<?> objectType, M marshaller) {
			this.objectType = objectType;
			this.marshaller = marshaller;
		}

		/**
         * Returns a boolean value indicating whether the JsonTesterFactoryBean is a singleton.
         * 
         * @return true if the JsonTesterFactoryBean is a singleton, false otherwise.
         */
        @Override
		public boolean isSingleton() {
			return false;
		}

		/**
         * Retrieves the object of type T.
         * 
         * @return the object of type T
         * @throws Exception if an error occurs while retrieving the object
         */
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

		/**
         * Returns the type of the object created by this factory bean.
         * 
         * @return the type of the object created by this factory bean
         */
        @Override
		public Class<?> getObjectType() {
			return this.objectType;
		}

	}

	/**
	 * {@link BeanPostProcessor} used to initialize JSON testers.
	 */
	static class JsonMarshalTestersBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

		/**
         * This method is called after the initialization of a bean. It processes the fields of the bean using reflection.
         * 
         * @param bean the initialized bean
         * @param beanName the name of the bean
         * @return the processed bean
         * @throws BeansException if an error occurs during the processing of the bean
         */
        @Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			ReflectionUtils.doWithFields(bean.getClass(), (field) -> processField(bean, field));
			return bean;
		}

		/**
         * Processes a field of a bean object.
         * 
         * @param bean the bean object
         * @param field the field to be processed
         */
        private void processField(Object bean, Field field) {
			if (AbstractJsonMarshalTester.class.isAssignableFrom(field.getType())) {
				initializeTester(bean, field, bean.getClass(), ResolvableType.forField(field).getGeneric());
			}
			else if (BasicJsonTester.class.isAssignableFrom(field.getType())) {
				initializeTester(bean, field, bean.getClass());
			}
		}

		/**
         * Initializes the tester object for the given bean and field.
         * 
         * @param bean the bean object
         * @param field the field object
         * @param args optional arguments for initialization
         */
        private void initializeTester(Object bean, Field field, Object... args) {
			ReflectionUtils.makeAccessible(field);
			Object tester = ReflectionUtils.getField(field, bean);
			if (tester != null) {
				ReflectionTestUtils.invokeMethod(tester, "initialize", args);
			}
		}

	}

	/**
     * AbstractJsonMarshalTesterRuntimeHints class.
     */
    @SuppressWarnings("rawtypes")
	static class AbstractJsonMarshalTesterRuntimeHints implements RuntimeHintsRegistrar {

		private final Class<? extends AbstractJsonMarshalTester> tester;

		/**
         * Constructs a new instance of the AbstractJsonMarshalTesterRuntimeHints class with the specified tester.
         * 
         * @param tester the class of the AbstractJsonMarshalTester to be used for runtime hints
         */
        AbstractJsonMarshalTesterRuntimeHints(Class<? extends AbstractJsonMarshalTester> tester) {
			this.tester = tester;
		}

		/**
         * Registers hints for the runtime behavior of the AbstractJsonMarshalTester class.
         * 
         * @param hints The runtime hints to register.
         * @param classLoader The class loader to use for reflection.
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			ReflectionHints reflection = hints.reflection();
			reflection.registerType(this.tester, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			reflection.registerMethod(
					ReflectionUtils.findMethod(this.tester, "initialize", Class.class, ResolvableType.class),
					ExecutableMode.INVOKE);
		}

	}

	/**
     * BasicJsonTesterRuntimeHints class.
     */
    static class BasicJsonTesterRuntimeHints implements RuntimeHintsRegistrar {

		/**
         * Registers hints for the BasicJsonTester class.
         * 
         * @param hints        the RuntimeHints object to register the hints with
         * @param classLoader  the ClassLoader to use for reflection
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			ReflectionHints reflection = hints.reflection();
			reflection.registerType(BasicJsonTester.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			reflection.registerMethod(ReflectionUtils.findMethod(BasicJsonTester.class, "initialize", Class.class),
					ExecutableMode.INVOKE);
		}

	}

}
