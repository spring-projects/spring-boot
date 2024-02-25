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

	/**
     * Constructs a new ConfigurationPropertiesJsr303Validator with the specified ApplicationContext.
     *
     * @param applicationContext the ApplicationContext to be used by the validator
     */
    ConfigurationPropertiesJsr303Validator(ApplicationContext applicationContext) {
		this.delegate = new Delegate(applicationContext);
	}

	/**
     * Check if the validator supports the given type.
     * 
     * @param type the type to be validated
     * @return true if the validator supports the type, false otherwise
     */
    @Override
	public boolean supports(Class<?> type) {
		return this.delegate.supports(type);
	}

	/**
     * Validates the given target object using the delegate validator.
     * 
     * @param target the object to be validated
     * @param errors the errors object to store any validation errors
     */
    @Override
	public void validate(Object target, Errors errors) {
		this.delegate.validate(target, errors);
	}

	/**
     * Checks if JSR 303 validation is present in the application context.
     * 
     * @param applicationContext the application context to check
     * @return true if JSR 303 validation is present, false otherwise
     */
    static boolean isJsr303Present(ApplicationContext applicationContext) {
		ClassLoader classLoader = applicationContext.getClassLoader();
		for (String validatorClass : VALIDATOR_CLASSES) {
			if (!ClassUtils.isPresent(validatorClass, classLoader)) {
				return false;
			}
		}
		return true;
	}

	/**
     * Delegate class.
     */
    private static class Delegate extends LocalValidatorFactoryBean {

		/**
         * Sets the application context and initializes the message interpolator.
         * 
         * @param applicationContext the application context to be set
         */
        Delegate(ApplicationContext applicationContext) {
			setApplicationContext(applicationContext);
			setMessageInterpolator(new MessageInterpolatorFactory(applicationContext).getObject());
			afterPropertiesSet();
		}

	}

}
