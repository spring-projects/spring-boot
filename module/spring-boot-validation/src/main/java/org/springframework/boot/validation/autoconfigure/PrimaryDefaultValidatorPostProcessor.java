/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
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
class PrimaryDefaultValidatorPostProcessor implements ImportBeanDefinitionRegistrar {

	/**
	 * The bean name of the auto-configured Validator.
	 */
	private static final String VALIDATOR_BEAN_NAME = "defaultValidator";

	private final @Nullable ConfigurableListableBeanFactory beanFactory;

	PrimaryDefaultValidatorPostProcessor(BeanFactory beanFactory) {
		this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory)
				? listableBeanFactory : null;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		BeanDefinition definition = getAutoConfiguredValidator(registry);
		if (definition != null) {
			definition.setPrimary(!hasPrimarySpringValidator());
		}
	}

	private @Nullable BeanDefinition getAutoConfiguredValidator(BeanDefinitionRegistry registry) {
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
		Assert.state(this.beanFactory != null, "'beanFactory' must not be null");
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
