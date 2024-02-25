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

package org.springframework.boot.autoconfigure.validation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Enable the {@code Primary} flag on the auto-configured validator if necessary.
 * <p>
 * As {@link LocalValidatorFactoryBean} exposes 3 validator related contracts and we're
 * only checking for the absence {@link jakarta.validation.Validator}, we should flag the
 * auto-configured validator as primary only if no Spring's {@link Validator} is flagged
 * as primary.
 *
 * @author Stephane Nicoll
 * @author Matej Nedic
 * @author Andy Wilkinson
 */
class PrimaryDefaultValidatorPostProcessor implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

	/**
	 * The bean name of the auto-configured Validator.
	 */
	private static final String VALIDATOR_BEAN_NAME = "defaultValidator";

	private ConfigurableListableBeanFactory beanFactory;

	/**
	 * Sets the bean factory for this post-processor.
	 * @param beanFactory the bean factory to set
	 * @throws BeansException if an error occurs while setting the bean factory
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory) {
			this.beanFactory = listableBeanFactory;
		}
	}

	/**
	 * Registers bean definitions for the PrimaryDefaultValidatorPostProcessor class.
	 *
	 * This method is responsible for registering the bean definitions for the
	 * PrimaryDefaultValidatorPostProcessor class. It takes in the importingClassMetadata
	 * and BeanDefinitionRegistry as parameters.
	 * @param importingClassMetadata the metadata of the importing class
	 * @param registry the registry for bean definitions
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		BeanDefinition definition = getAutoConfiguredValidator(registry);
		if (definition != null) {
			definition.setPrimary(!hasPrimarySpringValidator());
		}
	}

	/**
	 * Retrieves the auto-configured validator bean definition from the given bean
	 * definition registry.
	 * @param registry the bean definition registry to search for the validator bean
	 * definition
	 * @return the auto-configured validator bean definition, or null if not found or not
	 * of the expected type
	 */
	private BeanDefinition getAutoConfiguredValidator(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(VALIDATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(VALIDATOR_BEAN_NAME);
			if (definition.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE
					&& isTypeMatch(VALIDATOR_BEAN_NAME, LocalValidatorFactoryBean.class)) {
				return definition;
			}
		}
		return null;
	}

	/**
	 * Checks if the type of a bean with the given name matches the specified type.
	 * @param name the name of the bean to check
	 * @param type the type to match against
	 * @return true if the type of the bean matches the specified type, false otherwise
	 */
	private boolean isTypeMatch(String name, Class<?> type) {
		return this.beanFactory != null && this.beanFactory.isTypeMatch(name, type);
	}

	/**
	 * Checks if there is a primary Spring validator bean registered in the bean factory.
	 * @return true if a primary Spring validator bean is found, false otherwise
	 */
	private boolean hasPrimarySpringValidator() {
		String[] validatorBeans = this.beanFactory.getBeanNamesForType(Validator.class, false, false);
		for (String validatorBean : validatorBeans) {
			BeanDefinition definition = this.beanFactory.getBeanDefinition(validatorBean);
			if (definition.isPrimary()) {
				return true;
			}
		}
		return false;
	}

}
