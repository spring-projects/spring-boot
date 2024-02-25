/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationAotProcessor} for immutable configuration properties.
 *
 * @author Stephane Nicoll
 * @see ConstructorBound
 */
class ConfigurationPropertiesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	/**
     * Processes the given registered bean ahead of time.
     * 
     * @param registeredBean the registered bean to process
     * @return the BeanRegistrationAotContribution object, or null if the registered bean is not an immutable configuration properties bean definition
     */
    @Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (!isImmutableConfigurationPropertiesBeanDefinition(registeredBean.getMergedBeanDefinition())) {
			return null;
		}
		return BeanRegistrationAotContribution.withCustomCodeFragments(
				(codeFragments) -> new ConfigurationPropertiesBeanRegistrationCodeFragments(codeFragments,
						registeredBean));

	}

	/**
     * Determines if the given bean definition represents an immutable configuration properties bean definition.
     * 
     * @param beanDefinition the bean definition to check
     * @return true if the bean definition represents an immutable configuration properties bean definition, false otherwise
     */
    private boolean isImmutableConfigurationPropertiesBeanDefinition(BeanDefinition beanDefinition) {
		return BindMethod.VALUE_OBJECT.equals(BindMethodAttribute.get(beanDefinition));
	}

	/**
     * ConfigurationPropertiesBeanRegistrationCodeFragments class.
     */
    private static class ConfigurationPropertiesBeanRegistrationCodeFragments
			extends BeanRegistrationCodeFragmentsDecorator {

		private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

		private final RegisteredBean registeredBean;

		/**
         * Constructs a new ConfigurationPropertiesBeanRegistrationCodeFragments object with the specified
         * BeanRegistrationCodeFragments and RegisteredBean.
         * 
         * @param codeFragments the BeanRegistrationCodeFragments to be used
         * @param registeredBean the RegisteredBean to be used
         */
        ConfigurationPropertiesBeanRegistrationCodeFragments(BeanRegistrationCodeFragments codeFragments,
				RegisteredBean registeredBean) {
			super(codeFragments);
			this.registeredBean = registeredBean;
		}

		/**
         * Generates the code block for setting the properties of a bean definition based on the attributes of a configuration properties class.
         * 
         * @param generationContext The generation context.
         * @param beanRegistrationCode The bean registration code.
         * @param beanDefinition The root bean definition.
         * @param attributeFilter The attribute filter.
         * @return The code block for setting the bean definition properties.
         */
        @Override
		public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
				BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
				Predicate<String> attributeFilter) {
			return super.generateSetBeanDefinitionPropertiesCode(generationContext, beanRegistrationCode,
					beanDefinition, attributeFilter.or(BindMethodAttribute.NAME::equals));
		}

		/**
         * Returns the target class name for the given registered bean.
         * 
         * @param registeredBean the registered bean
         * @return the target class name
         */
        @Override
		public ClassName getTarget(RegisteredBean registeredBean) {
			return ClassName.get(this.registeredBean.getBeanClass());
		}

		/**
         * Generates the code for the instance supplier method.
         *
         * @param generationContext The generation context.
         * @param beanRegistrationCode The bean registration code.
         * @param allowDirectSupplierShortcut Flag indicating whether to allow direct supplier shortcut.
         * @return The code block representing the instance supplier method.
         */
        @Override
		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
				BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", (method) -> {
				Class<?> beanClass = this.registeredBean.getBeanClass();
				method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName())
					.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
					.returns(beanClass)
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
