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

package org.springframework.boot.test.autoconfigure.json;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.test.json.AbstractJsonMarshalTester;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ResolvableType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Auto-configuration for Json testers.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @since 1.4.0
 * @see AutoConfigureJsonTesters
 */
@AutoConfiguration
@ConditionalOnJsonTesters
public final class JsonTestersAutoConfiguration {

	@Bean
	static JsonMarshalTestersBeanPostProcessor jsonMarshalTestersBeanPostProcessor() {
		return new JsonMarshalTestersBeanPostProcessor();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ImportRuntimeHints(BasicJsonTesterRuntimeHints.class)
	FactoryBean<BasicJsonTester> basicJsonTesterFactoryBean() {
		return new JsonTesterFactoryBean<BasicJsonTester, Void>(BasicJsonTester.class, null);
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

	static class BasicJsonTesterRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			ReflectionHints reflection = hints.reflection();
			reflection.registerType(BasicJsonTester.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			Method method = ReflectionUtils.findMethod(BasicJsonTester.class, "initialize", Class.class);
			Assert.state(method != null, "'method' must not be null");
			reflection.registerMethod(method, ExecutableMode.INVOKE);
		}

	}

}
