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

package org.springframework.boot.autoconfigure.validation;

import jakarta.validation.ValidationException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

/**
 * {@link Validator} implementation that delegates calls to another {@link Validator}.
 * This {@link Validator} implements Spring's {@link SmartValidator} interface but does
 * not implement the JSR-303 {@code jakarta.validator.Validator} interface.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Zisis Pavloudis
 * @since 2.0.0
 */
public class ValidatorAdapter implements SmartValidator, ApplicationContextAware, InitializingBean, DisposableBean {

	private final SmartValidator target;

	private final boolean existingBean;

	/**
	 * Constructs a new ValidatorAdapter with the specified SmartValidator target and
	 * existingBean flag.
	 * @param target the SmartValidator to be adapted
	 * @param existingBean flag indicating whether the target SmartValidator is an
	 * existing bean or not
	 */
	ValidatorAdapter(SmartValidator target, boolean existingBean) {
		this.target = target;
		this.existingBean = existingBean;
	}

	/**
	 * Returns the target Validator object.
	 * @return the target Validator object
	 */
	public final Validator getTarget() {
		return this.target;
	}

	/**
	 * Returns a boolean value indicating whether the ValidatorAdapter supports the given
	 * class.
	 * @param clazz the class to be checked for support
	 * @return true if the ValidatorAdapter supports the given class, false otherwise
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return this.target.supports(clazz);
	}

	/**
	 * Validates the specified target object using the provided Errors object.
	 * @param target the object to be validated
	 * @param errors the Errors object to store any validation errors
	 */
	@Override
	public void validate(Object target, Errors errors) {
		this.target.validate(target, errors);
	}

	/**
	 * Validates the given target object using the specified validation hints.
	 * @param target the object to be validated
	 * @param errors the errors object to store any validation errors
	 * @param validationHints optional validation hints to be used during validation
	 */
	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		this.target.validate(target, errors, validationHints);
	}

	/**
	 * Sets the application context for this ValidatorAdapter.
	 * @param applicationContext the ApplicationContext to be set
	 * @throws BeansException if an error occurs while setting the application context
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (!this.existingBean && this.target instanceof ApplicationContextAware contextAwareTarget) {
			contextAwareTarget.setApplicationContext(applicationContext);
		}
	}

	/**
	 * This method is called after all bean properties have been set, and performs any
	 * necessary initialization. If the target object is an instance of InitializingBean,
	 * the afterPropertiesSet() method of the target object is called.
	 * @throws Exception if an error occurs during initialization
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (!this.existingBean && this.target instanceof InitializingBean initializingBean) {
			initializingBean.afterPropertiesSet();
		}
	}

	/**
	 * This method is called when the ValidatorAdapter is being destroyed. It checks if
	 * the target bean is an instance of DisposableBean and if it is not an existing bean.
	 * If both conditions are met, it calls the destroy() method of the DisposableBean.
	 * @throws Exception if an error occurs during the destruction process
	 */
	@Override
	public void destroy() throws Exception {
		if (!this.existingBean && this.target instanceof DisposableBean disposableBean) {
			disposableBean.destroy();
		}
	}

	/**
	 * Return a {@link Validator} that only implements the {@link Validator} interface,
	 * wrapping it if necessary.
	 * <p>
	 * If the specified {@link Validator} is not {@code null}, it is wrapped. If not, a
	 * {@link jakarta.validation.Validator} is retrieved from the context and wrapped.
	 * Otherwise, a new default validator is created.
	 * @param applicationContext the application context
	 * @param validator an existing validator to use or {@code null}
	 * @return the validator to use
	 */
	public static Validator get(ApplicationContext applicationContext, Validator validator) {
		if (validator != null) {
			return wrap(validator, false);
		}
		return getExistingOrCreate(applicationContext);
	}

	/**
	 * Retrieves an existing Validator instance from the ApplicationContext if available,
	 * otherwise creates a new Validator instance.
	 * @param applicationContext the ApplicationContext to retrieve the Validator from
	 * @return the existing Validator instance if available, otherwise a new Validator
	 * instance
	 */
	private static Validator getExistingOrCreate(ApplicationContext applicationContext) {
		Validator existing = getExisting(applicationContext);
		if (existing != null) {
			return wrap(existing, true);
		}
		return create(applicationContext);
	}

	/**
	 * Retrieves an existing validator from the application context.
	 * @param applicationContext the application context from which to retrieve the
	 * validator
	 * @return the existing validator if found, or null if not found
	 */
	private static Validator getExisting(ApplicationContext applicationContext) {
		try {
			jakarta.validation.Validator validatorBean = applicationContext.getBean(jakarta.validation.Validator.class);
			if (validatorBean instanceof Validator validator) {
				return validator;
			}
			return new SpringValidatorAdapter(validatorBean);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	/**
	 * Creates a new Validator instance with the given MessageSource.
	 * @param messageSource the MessageSource to be used for message interpolation
	 * @return a new Validator instance
	 */
	private static Validator create(MessageSource messageSource) {
		OptionalValidatorFactoryBean validator = new OptionalValidatorFactoryBean();
		try {
			MessageInterpolatorFactory factory = new MessageInterpolatorFactory(messageSource);
			validator.setMessageInterpolator(factory.getObject());
		}
		catch (ValidationException ex) {
			// Ignore
		}
		return wrap(validator, false);
	}

	/**
	 * Wraps the given validator with the specified existing bean flag.
	 * @param validator The validator to be wrapped.
	 * @param existingBean The flag indicating whether the validator is for an existing
	 * bean.
	 * @return The wrapped validator.
	 */
	private static Validator wrap(Validator validator, boolean existingBean) {
		if (validator instanceof jakarta.validation.Validator jakartaValidator) {
			if (jakartaValidator instanceof SpringValidatorAdapter adapter) {
				return new ValidatorAdapter(adapter, existingBean);
			}
			return new ValidatorAdapter(new SpringValidatorAdapter(jakartaValidator), existingBean);
		}
		return validator;
	}

	/**
	 * Returns an object of the specified type if the underlying target object is an
	 * instance of the specified type.
	 * @param type the class object representing the type to be unwrapped
	 * @return an object of the specified type if the underlying target object is an
	 * instance of the specified type, or the result of calling the unwrap method on the
	 * underlying target object
	 * @throws NullPointerException if the specified type is null
	 * @throws ClassCastException if the underlying target object is not an instance of
	 * the specified type
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if (type.isInstance(this.target)) {
			return (T) this.target;
		}
		return this.target.unwrap(type);
	}

}
