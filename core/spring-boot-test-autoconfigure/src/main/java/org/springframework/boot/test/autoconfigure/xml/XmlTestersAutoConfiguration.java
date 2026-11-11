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

package org.springframework.boot.test.autoconfigure.xml;

import java.lang.reflect.Field;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.test.xml.AbstractXmlMarshalTester;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Auto-configuration for XML testers.
 *
 * @author Tiziano Basile
 * @since 4.2.0
 * @see AutoConfigureXmlTesters
 */
@AutoConfiguration
@ConditionalOnXmlTesters
public final class XmlTestersAutoConfiguration {

	@Bean
	static XmlMarshalTestersBeanPostProcessor xmlMarshalTestersBeanPostProcessor() {
		return new XmlMarshalTestersBeanPostProcessor();
	}

	/**
	 * {@link BeanPostProcessor} used to initialize XML testers.
	 */
	static class XmlMarshalTestersBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			ReflectionUtils.doWithFields(bean.getClass(), (field) -> processField(bean, field));
			return bean;
		}

		private void processField(Object bean, Field field) {
			if (AbstractXmlMarshalTester.class.isAssignableFrom(field.getType())) {
				ReflectionUtils.makeAccessible(field);
				Object tester = ReflectionUtils.getField(field, bean);
				if (tester != null) {
					ReflectionTestUtils.invokeMethod(tester, "initialize", bean.getClass(), getTypeUnderTest(field));
				}
			}
		}

		private ResolvableType getTypeUnderTest(Field field) {
			ResolvableType type = ResolvableType.forField(field).getGeneric();
			Assert.state(type.resolve() != null,
					() -> "Unable to determine the type under test for field '" + field.getName() + "' of "
							+ field.getDeclaringClass().getName() + ". Declare the field with an explicit generic "
							+ "type, for example '" + field.getType().getSimpleName() + "<MyType> " + field.getName()
							+ ";'.");
			return type;
		}

	}

}
