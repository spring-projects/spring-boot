/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Validator that supports configuration classes annotated with
 * {@link Validated @Validated}.
 *
 * @author Phillip Webb
 */
final class ConfigurationPropertiesJsr303Validator implements Validator {

	private static final String[] VALIDATOR_CLASSES = { "jakarta.validation.Validator",
			"jakarta.validation.ValidatorFactory", "jakarta.validation.bootstrap.GenericBootstrap" };

	private final Delegate delegate;

	private final Class<?> validatedType;

	ConfigurationPropertiesJsr303Validator(ApplicationContext applicationContext, Class<?> validatedType) {
		this.delegate = new Delegate(applicationContext);
		this.validatedType = validatedType;
	}

	@Override
	public boolean supports(Class<?> type) {
		return this.validatedType.equals(type) && this.delegate.supports(type);
	}

	@Override
	public void validate(Object target, Errors errors) {
		this.delegate.validate(target, errors);
	}

	static boolean isJsr303Present(ApplicationContext applicationContext) {
		ClassLoader classLoader = applicationContext.getClassLoader();
		for (String validatorClass : VALIDATOR_CLASSES) {
			if (!ClassUtils.isPresent(validatorClass, classLoader)) {
				return false;
			}
		}
		return true;
	}

	private static class Delegate extends LocalValidatorFactoryBean {

		Delegate(ApplicationContext applicationContext) {
			setApplicationContext(applicationContext);
			setMessageInterpolator(new MessageInterpolatorFactory(applicationContext).getObject());
			afterPropertiesSet();
		}

	}

}
