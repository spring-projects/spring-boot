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

package org.springframework.boot.context.properties;

import java.lang.reflect.Executable;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;
import org.springframework.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationAotProcessor} for immutable configuration properties.
 *
 * @author Stephane Nicoll
 * @see ConstructorBound
 */
class ConfigurationPropertiesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (!isImmutableConfigurationPropertiesBeanDefinition(registeredBean.getMergedBeanDefinition())) {
			return null;
		}
		return BeanRegistrationAotContribution.ofBeanRegistrationCodeFragmentsCustomizer(
				(codeFragments) -> new ConfigurationPropertiesBeanRegistrationCodeFragments(codeFragments,
						registeredBean));

	}

	private boolean isImmutableConfigurationPropertiesBeanDefinition(BeanDefinition beanDefinition) {
		return beanDefinition.hasAttribute(BindMethod.class.getName())
				&& BindMethod.VALUE_OBJECT.equals(beanDefinition.getAttribute(BindMethod.class.getName()));
	}

	private static class ConfigurationPropertiesBeanRegistrationCodeFragments extends BeanRegistrationCodeFragments {

		private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

		private final RegisteredBean registeredBean;

		ConfigurationPropertiesBeanRegistrationCodeFragments(BeanRegistrationCodeFragments codeFragments,
				RegisteredBean registeredBean) {
			super(codeFragments);
			this.registeredBean = registeredBean;
		}

		@Override
		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
				BeanRegistrationCode beanRegistrationCode, Executable constructorOrFactoryMethod,
				boolean allowDirectSupplierShortcut) {
			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", (method) -> {
				Class<?> beanClass = this.registeredBean.getBeanClass();
				method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName())
						.addModifiers(Modifier.PRIVATE, Modifier.STATIC).returns(beanClass)
						.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME)
						.addStatement("$T beanFactory = registeredBean.getBeanFactory()", BeanFactory.class)
						.addStatement("$T beanName = registeredBean.getBeanName()", String.class)
						.addStatement("$T<?> beanClass = registeredBean.getBeanClass()", Class.class)
						.addStatement("return ($T) $T.from(beanFactory, beanName, beanClass)", beanClass,
								ConstructorBound.class);
			});
			return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, beanRegistrationCode.getClassName(),
					generatedMethod.getName());
		}

	}

}
