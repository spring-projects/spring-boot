/*
 * Copyright 2012-2021 the original author or authors.
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
 * only checking for the absence {@link javax.validation.Validator}, we should flag the
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

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		BeanDefinition definition = getAutoConfiguredValidator(registry);
		if (definition != null) {
			definition.setPrimary(!hasPrimarySpringValidator());
		}
	}

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

	private boolean isTypeMatch(String name, Class<?> type) {
		return this.beanFactory != null && this.beanFactory.isTypeMatch(name, type);
	}

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
